package com.camp.app.service.impl;

import com.camp.app.api.BizException;
import com.camp.app.api.PageResult;
import com.camp.app.entity.OaFileShare;
import com.camp.app.entity.StoredFile;
import com.camp.app.model.FileVo;
import com.camp.app.repository.OaFileShareRepository;
import com.camp.app.repository.RoleRepository;
import com.camp.app.repository.StoredFileRepository;
import com.camp.app.repository.UserRepository;
import com.camp.app.security.AuthContext;
import com.camp.app.security.AuthPrincipal;
import com.camp.app.security.Phase1Roles;
import com.camp.app.service.FileStorageService;
import com.camp.app.storage.BlobStore;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 字节交给 BlobStore，元数据写入已有的 oa_files。
 * storage_key 第一段是用途；目录用 dir/{ownerId}/{uuid}，不落字节。
 */
@Service
public class FileStorageServiceImpl implements FileStorageService {
    static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Set<String> IMAGES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");
    private static final Set<String> DOCUMENTS = Set.of("application/pdf", "text/plain");
    private static final Set<String> CSV = Set.of("text/csv", "text/plain", "application/csv");
    private static final Set<String> PURPOSES = Set.of(
            "AVATAR", "ANNOUNCEMENT", "OTHER", "STUDENT_IMPORT", "STUDENT_EXPORT");
    private static final Set<String> PUBLIC_PURPOSES = Set.of("AVATAR", "ANNOUNCEMENT");

    private final StoredFileRepository storedFileRepository;
    private final OaFileShareRepository shareRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BlobStore blobStore;

    public FileStorageServiceImpl(StoredFileRepository storedFileRepository,
                                  OaFileShareRepository shareRepository,
                                  UserRepository userRepository,
                                  RoleRepository roleRepository,
                                  BlobStore blobStore) {
        this.storedFileRepository = storedFileRepository;
        this.shareRepository = shareRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.blobStore = blobStore;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<FileVo> list(long page, long size, Long parentId, String purpose, String scope) {
        AuthPrincipal principal = requirePrincipal();
        String normalizedScope = normalizeScope(scope);
        if ("all".equals(normalizedScope) && !admin(principal)) {
            throw BizException.forbidden("只有管理员可以查看全部文件");
        }
        String purposeLike = purposeLike(purpose);
        if ("shared".equals(normalizedScope)) {
            return listShared(principal, page, size, parentId, purposeLike);
        }
        boolean anyOwner = "all".equals(normalizedScope);
        assertListParent(parentId, principal.userId(), anyOwner);
        int parentMode = parentId == null ? 0 : 1;
        Page<StoredFile> result = storedFileRepository.search(
                anyOwner ? 1 : 0,
                anyOwner ? 0L : principal.userId(),
                parentMode,
                parentId == null ? 0L : parentId,
                purposeLike,
                PageRequest.of((int) (page - 1), (int) size, listSort()));
        return PageResult.<FileVo>builder()
                .page(page)
                .size(size)
                .total(result.getTotalElements())
                .records(result.getContent().stream().map(this::toVo).toList())
                .build();
    }

    @Override
    @Transactional
    public FileVo upload(MultipartFile file, Long uploaderId, String purpose, Long parentId) {
        if (uploaderId == null) {
            throw BizException.unauthorized("未登录");
        }
        if (!userRepository.existsById(uploaderId)) {
            throw BizException.notFound("上传人不存在");
        }
        if (file == null || file.isEmpty()) {
            throw BizException.badRequest("文件不能为空");
        }
        if (file.getSize() > MAX_BYTES) {
            throw BizException.badRequest("文件不能超过 5MB");
        }
        String normalizedPurpose = normalizePurpose(purpose);
        String contentType = baseType(file.getContentType());
        if (!allowed(normalizedPurpose, contentType)) {
            throw BizException.unprocessable("不支持的文件类型");
        }
        assertOwnDirectory(parentId, uploaderId);
        try (InputStream input = file.getInputStream()) {
            return persist(uploaderId, normalizedPurpose, sanitizeOriginal(file.getOriginalFilename()),
                    contentType, file.getSize(), parentId, input);
        } catch (IOException ex) {
            throw BizException.badRequest("文件保存失败");
        }
    }

    @Override
    @Transactional
    public FileVo createFolder(String name, Long parentId) {
        AuthPrincipal principal = requirePrincipal();
        if (!userRepository.existsById(principal.userId())) {
            throw BizException.notFound("上传人不存在");
        }
        if (name == null || name.isBlank()) {
            throw BizException.badRequest("目录名不能为空");
        }
        String folderName = sanitizeOriginal(name.trim());
        assertOwnDirectory(parentId, principal.userId());
        if (siblingNameTaken(principal.userId(), parentId, folderName)) {
            throw BizException.conflict("同目录下已有同名目录或文件");
        }
        String objectName = UUID.randomUUID().toString();
        StoredFile row = new StoredFile();
        row.setOwnerId(principal.userId());
        row.setParentId(parentId);
        row.setName(folderName);
        row.setStorageKey("dir/" + principal.userId() + "/" + objectName);
        row.setMime(null);
        row.setSizeBytes(0L);
        row.setDirectory(1);
        row = storedFileRepository.save(row);
        return toVo(row);
    }

    @Override
    @Transactional
    public FileVo storeBytes(Long ownerId, String purpose, String originalName, String contentType, byte[] body) {
        if (ownerId == null) {
            throw BizException.unauthorized("未登录");
        }
        if (!userRepository.existsById(ownerId)) {
            throw BizException.notFound("上传人不存在");
        }
        if (body == null || body.length == 0) {
            throw BizException.badRequest("文件不能为空");
        }
        if (body.length > MAX_BYTES) {
            throw BizException.badRequest("文件不能超过 5MB");
        }
        String normalizedPurpose = normalizePurpose(purpose);
        String normalizedType = baseType(contentType);
        if (!allowed(normalizedPurpose, normalizedType)) {
            throw BizException.unprocessable("不支持的文件类型");
        }
        return persist(ownerId, normalizedPurpose, sanitizeOriginal(originalName),
                normalizedType, body.length, null, new ByteArrayInputStream(body));
    }

    @Override
    @Transactional(readOnly = true)
    public String readUtf8(Long fileId) {
        StoredFile row = requireReadable(fileId);
        if (isDir(row)) {
            throw BizException.unprocessable("目录不能作为导入文件");
        }
        String mime = baseType(row.getMime());
        String purpose = purposeOf(row.getStorageKey());
        if (!"STUDENT_IMPORT".equals(purpose) && !"STUDENT_EXPORT".equals(purpose) && !CSV.contains(mime)) {
            throw BizException.unprocessable("只能导入 CSV 或文本文件");
        }
        Resource resource = readBlob(row);
        try (InputStream input = resource.getInputStream()) {
            byte[] bytes = input.readAllBytes();
            if (bytes.length > MAX_BYTES) {
                throw BizException.badRequest("文件不能超过 5MB");
            }
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw BizException.badRequest("文件读取失败");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Resource load(Long fileId) {
        StoredFile row = requireReadable(fileId);
        if (isDir(row)) {
            throw BizException.unprocessable("目录不能下载");
        }
        return readBlob(row);
    }

    @Override
    @Transactional(readOnly = true)
    public String contentType(Long fileId) {
        String type = requireReadable(fileId).getMime();
        return type == null || type.isBlank() ? "application/octet-stream" : type;
    }

    @Override
    @Transactional(readOnly = true)
    public String downloadName(Long fileId) {
        String name = requireReadable(fileId).getName();
        return name == null || name.isBlank() ? "file" : name;
    }

    @Override
    @Transactional
    public void delete(Long fileId, Long operatorId, boolean admin) {
        StoredFile row = require(fileId);
        if (!admin && (operatorId == null || !operatorId.equals(row.getOwnerId()))) {
            throw BizException.forbidden("只能删除自己上传的文件");
        }
        if (storedFileRepository.existsByParentId(fileId)) {
            throw BizException.conflict("目录不为空，不能删除");
        }
        if (shareRepository.existsByFileId(fileId)) {
            throw BizException.conflict("文件已分享，不能删除");
        }
        if (!isDir(row)) {
            try {
                blobStore.delete(objectName(row));
            } catch (IOException ex) {
                throw BizException.badRequest("文件删除失败");
            } catch (IllegalArgumentException ex) {
                throw BizException.notFound("文件不存在");
            }
        }
        storedFileRepository.delete(row);
    }

    @Override
    @Transactional(readOnly = true)
    public void assertReferenced(String url) {
        if (url == null || url.isBlank() || !url.startsWith("/api/v1/files/")) {
            return;
        }
        String tail = url.substring("/api/v1/files/".length());
        int slash = tail.indexOf('/');
        if (slash >= 0) {
            tail = tail.substring(0, slash);
        }
        try {
            require(Long.valueOf(tail));
        } catch (NumberFormatException ex) {
            throw BizException.badRequest("附件地址不是有效的 fileId");
        }
    }

    private FileVo persist(Long ownerId, String purpose, String original, String contentType, long size,
                           Long parentId, InputStream input) {
        String storedName = UUID.randomUUID() + extensionOf(original, contentType);
        try {
            blobStore.write(storedName, input);
        } catch (IOException | IllegalArgumentException ex) {
            throw BizException.badRequest("文件保存失败");
        }
        StoredFile row = new StoredFile();
        row.setOwnerId(ownerId);
        row.setParentId(parentId);
        row.setName(original);
        row.setStorageKey(purpose + "/" + storedName);
        row.setMime(contentType);
        row.setSizeBytes(size);
        row.setDirectory(0);
        try {
            row = storedFileRepository.save(row);
        } catch (RuntimeException ex) {
            try {
                blobStore.delete(storedName);
            } catch (IOException ignored) {
                // 元数据没写上时尽量清掉刚落下的字节
            }
            throw ex;
        }
        return toVo(row);
    }

    private PageResult<FileVo> listShared(AuthPrincipal principal, long page, long size,
                                          Long parentId, String purposeLike) {
        LocalDateTime now = LocalDateTime.now();
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        for (OaFileShare share : shareRepository.findByShareToUserId(principal.userId())) {
            if (active(share, now)) {
                ids.add(share.getFileId());
            }
        }
        for (Long roleId : roleIds(principal)) {
            for (OaFileShare share : shareRepository.findByShareToRoleId(roleId)) {
                if (active(share, now)) {
                    ids.add(share.getFileId());
                }
            }
        }
        List<FileVo> matched = new ArrayList<>();
        if (!ids.isEmpty()) {
            List<StoredFile> rows = storedFileRepository.findAllById(ids);
            rows.stream()
                    .filter(row -> row.getDeletedAt() == null)
                    .filter(row -> parentId == null || parentId.equals(row.getParentId()))
                    .filter(row -> purposeLike.isEmpty() || (row.getStorageKey() != null && row.getStorageKey().startsWith(purposePrefix(purposeLike))))
                    .sorted(Comparator.comparing((StoredFile row) -> isDir(row) ? 0 : 1)
                            .thenComparing(row -> row.getName() == null ? "" : row.getName()))
                    .map(this::toVo)
                    .forEach(matched::add);
        }
        int from = (int) Math.min((page - 1) * size, matched.size());
        int to = (int) Math.min(from + size, matched.size());
        return PageResult.<FileVo>builder()
                .page(page)
                .size(size)
                .total(matched.size())
                .records(new ArrayList<>(matched.subList(from, to)))
                .build();
    }

    private void assertListParent(Long parentId, Long ownerId, boolean anyOwner) {
        if (parentId == null) {
            return;
        }
        StoredFile parent = directory(parentId);
        if (!anyOwner && !ownerId.equals(parent.getOwnerId())) {
            throw BizException.forbidden("不能查看他人的目录");
        }
    }

    private void assertOwnDirectory(Long parentId, Long ownerId) {
        if (parentId == null) {
            return;
        }
        StoredFile parent = directory(parentId);
        if (!ownerId.equals(parent.getOwnerId())) {
            throw BizException.forbidden("只能写入自己的目录");
        }
    }

    private StoredFile directory(Long parentId) {
        StoredFile parent = storedFileRepository.findById(parentId)
                .orElseThrow(() -> BizException.notFound("目录不存在"));
        if (parent.getDeletedAt() != null || !isDir(parent)) {
            throw BizException.badRequest("parentId 必须是未删除的目录");
        }
        return parent;
    }

    private boolean siblingNameTaken(Long ownerId, Long parentId, String name) {
        Page<StoredFile> page = storedFileRepository.search(
                0, ownerId, parentId == null ? 0 : 1, parentId == null ? 0L : parentId, "",
                PageRequest.of(0, 200, listSort()));
        for (StoredFile row : page.getContent()) {
            if (name.equals(row.getName())) {
                return true;
            }
        }
        return page.getTotalElements() > page.getContent().size()
                && storedFileRepository.search(
                        0, ownerId, parentId == null ? 0 : 1, parentId == null ? 0L : parentId, "",
                        PageRequest.of(0, (int) Math.min(page.getTotalElements(), 500), listSort()))
                .getContent().stream().anyMatch(row -> name.equals(row.getName()));
    }

    private Resource readBlob(StoredFile row) {
        String objectName = objectName(row);
        try {
            if (!blobStore.exists(objectName)) {
                throw BizException.notFound("文件不存在");
            }
            return blobStore.read(objectName);
        } catch (IllegalArgumentException ex) {
            throw BizException.notFound("文件不存在");
        }
    }

    private StoredFile requireReadable(Long fileId) {
        StoredFile row = require(fileId);
        AuthPrincipal principal = requirePrincipal();
        if (!canRead(row, principal)) {
            throw BizException.forbidden("无权访问该文件");
        }
        return row;
    }

    private boolean canRead(StoredFile row, AuthPrincipal principal) {
        if (admin(principal) || principal.userId().equals(row.getOwnerId())) {
            return true;
        }
        if (PUBLIC_PURPOSES.contains(purposeOf(row.getStorageKey()))) {
            return true;
        }
        return sharedWith(row.getId(), principal);
    }

    private boolean sharedWith(Long fileId, AuthPrincipal principal) {
        LocalDateTime now = LocalDateTime.now();
        List<Long> roles = roleIds(principal);
        for (OaFileShare share : shareRepository.findByFileId(fileId)) {
            if (!active(share, now)) {
                continue;
            }
            if (principal.userId().equals(share.getShareToUserId())) {
                return true;
            }
            if (share.getShareToRoleId() != null && roles.contains(share.getShareToRoleId())) {
                return true;
            }
        }
        return false;
    }

    private boolean active(OaFileShare share, LocalDateTime now) {
        return share.getExpiresAt() == null || share.getExpiresAt().isAfter(now);
    }

    private List<Long> roleIds(AuthPrincipal principal) {
        if (principal.roles() == null || principal.roles().isEmpty()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (String code : principal.roles()) {
            roleRepository.findByRoleCode(code).ifPresent(role -> ids.add(role.getId()));
        }
        return ids;
    }

    private StoredFile require(Long fileId) {
        if (fileId == null) {
            throw BizException.notFound("文件不存在");
        }
        StoredFile row = storedFileRepository.findById(fileId)
                .orElseThrow(() -> BizException.notFound("文件不存在"));
        if (row.getDeletedAt() != null) {
            throw BizException.notFound("文件不存在");
        }
        return row;
    }

    private AuthPrincipal requirePrincipal() {
        AuthPrincipal principal = AuthContext.get();
        if (principal == null || principal.userId() == null) {
            throw BizException.unauthorized("未登录");
        }
        return principal;
    }

    private boolean admin(AuthPrincipal principal) {
        return principal.roles() != null && principal.roles().contains(Phase1Roles.ADMIN);
    }

    private boolean isDir(StoredFile row) {
        return row.getDirectory() != null && row.getDirectory() == 1;
    }

    private String objectName(StoredFile row) {
        String key = row.getStorageKey() == null ? "" : row.getStorageKey();
        int slash = key.lastIndexOf('/');
        String fileName = slash >= 0 ? key.substring(slash + 1) : key;
        if (fileName.isBlank()) {
            throw BizException.notFound("文件不存在");
        }
        return fileName;
    }

    private FileVo toVo(StoredFile row) {
        boolean directory = isDir(row);
        return FileVo.builder()
                .fileId(row.getId())
                .url(directory ? null : "/api/v1/files/" + row.getId())
                .originalName(row.getName())
                .contentType(row.getMime())
                .sizeBytes(row.getSizeBytes())
                .purpose(directory ? null : purposeOf(row.getStorageKey()))
                .parentId(row.getParentId())
                .directory(directory)
                .ownerId(row.getOwnerId())
                .createdAt(row.getCreatedAt())
                .build();
    }

    private String purposeOf(String storageKey) {
        if (storageKey == null) {
            return "OTHER";
        }
        int slash = storageKey.indexOf('/');
        if (slash <= 0) {
            return "OTHER";
        }
        String head = storageKey.substring(0, slash);
        return PURPOSES.contains(head) ? head : "OTHER";
    }

    private String purposePrefix(String purposeLike) {
        int slash = purposeLike.indexOf('/');
        return slash < 0 ? purposeLike : purposeLike.substring(0, slash + 1);
    }

    private String normalizeScope(String scope) {
        if (scope == null || scope.isBlank()) {
            return "mine";
        }
        String value = scope.trim().toLowerCase(Locale.ROOT);
        if (!Set.of("mine", "shared", "all").contains(value)) {
            throw BizException.badRequest("scope 只能是 mine、shared 或 all");
        }
        return value;
    }

    private String purposeLike(String purpose) {
        if (purpose == null || purpose.isBlank()) {
            return "";
        }
        return normalizePurpose(purpose) + "/%";
    }

    private String normalizePurpose(String purpose) {
        if (purpose == null || purpose.isBlank()) {
            return "OTHER";
        }
        String value = purpose.trim().toUpperCase(Locale.ROOT);
        if (!PURPOSES.contains(value)) {
            throw BizException.badRequest("purpose 只能是 AVATAR、ANNOUNCEMENT、OTHER、STUDENT_IMPORT 或 STUDENT_EXPORT");
        }
        return value;
    }

    private boolean allowed(String purpose, String contentType) {
        if ("AVATAR".equals(purpose)) {
            return IMAGES.contains(contentType);
        }
        if ("STUDENT_IMPORT".equals(purpose) || "STUDENT_EXPORT".equals(purpose)) {
            return CSV.contains(contentType);
        }
        return IMAGES.contains(contentType) || DOCUMENTS.contains(contentType) || CSV.contains(contentType);
    }

    private String baseType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "";
        }
        String value = contentType.toLowerCase(Locale.ROOT).trim();
        int semi = value.indexOf(';');
        if (semi >= 0) {
            value = value.substring(0, semi).trim();
        }
        return value;
    }

    private String sanitizeOriginal(String name) {
        if (name == null || name.isBlank()) {
            return "file";
        }
        String cleaned = name.replace('\\', '/');
        int slash = cleaned.lastIndexOf('/');
        if (slash >= 0) {
            cleaned = cleaned.substring(slash + 1);
        }
        cleaned = cleaned.replaceAll("[\\r\\n\\t]", "");
        if (cleaned.isBlank()) {
            return "file";
        }
        return cleaned.length() > 255 ? cleaned.substring(0, 255) : cleaned;
    }

    private String extensionOf(String original, String contentType) {
        int dot = original.lastIndexOf('.');
        if (dot >= 0 && dot < original.length() - 1) {
            String ext = original.substring(dot + 1).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
            if (!ext.isBlank() && ext.length() <= 8) {
                return "." + ext;
            }
        }
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "application/pdf" -> ".pdf";
            case "text/plain" -> ".txt";
            case "text/csv", "application/csv" -> ".csv";
            default -> "";
        };
    }

    private Sort listSort() {
        return Sort.by(Sort.Order.desc("directory"), Sort.Order.asc("name"), Sort.Order.asc("id"));
    }
}
