package com.camp.app.service.impl;

import com.camp.app.api.BizException;
import com.camp.app.entity.StoredFile;
import com.camp.app.model.FileVo;
import com.camp.app.repository.StoredFileRepository;
import com.camp.app.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 字节落在 camp.upload.dir，元数据在 sys_files。H2 与 MySQL 共用这张表。
 */
@Service
public class FileStorageServiceImpl implements FileStorageService {
    static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Set<String> IMAGES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");
    private static final Set<String> DOCUMENTS = Set.of("application/pdf", "text/plain");

    private final StoredFileRepository storedFileRepository;
    private final Path dir;

    public FileStorageServiceImpl(StoredFileRepository storedFileRepository,
                                  @Value("${camp.upload.dir:data/uploads}") String uploadDir) {
        this.storedFileRepository = storedFileRepository;
        this.dir = Path.of(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.dir);
        } catch (IOException ex) {
            throw new IllegalStateException("无法准备上传目录 " + this.dir, ex);
        }
    }

    @Override
    public FileVo upload(MultipartFile file, Long uploaderId, String purpose) {
        if (uploaderId == null) {
            throw BizException.unauthorized("未登录");
        }
        if (file == null || file.isEmpty()) {
            throw BizException.badRequest("文件不能为空");
        }
        if (file.getSize() > MAX_BYTES) {
            throw BizException.badRequest("文件不能超过 5MB");
        }
        String normalizedPurpose = normalizePurpose(purpose);
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!allowed(normalizedPurpose, contentType)) {
            throw BizException.unprocessable("不支持的文件类型");
        }
        String original = sanitizeOriginal(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + extensionOf(original, contentType);
        Path target = dir.resolve(storedName).normalize();
        if (!target.startsWith(dir)) {
            throw BizException.badRequest("文件名不合法");
        }
        try (InputStream input = file.getInputStream()) {
            Files.copy(input, target);
        } catch (IOException ex) {
            throw BizException.badRequest("文件保存失败");
        }
        StoredFile row = new StoredFile();
        row.setStoredName(storedName);
        row.setOriginalName(original);
        row.setContentType(contentType);
        row.setSizeBytes(file.getSize());
        row.setPurpose(normalizedPurpose);
        row.setUploaderId(uploaderId);
        row = storedFileRepository.save(row);
        return toVo(row);
    }

    @Override
    public Resource load(Long fileId) {
        StoredFile row = require(fileId);
        Path path = dir.resolve(row.getStoredName()).normalize();
        if (!path.startsWith(dir) || !Files.exists(path)) {
            throw BizException.notFound("文件不存在");
        }
        return new FileSystemResource(path);
    }

    @Override
    public String contentType(Long fileId) {
        String type = require(fileId).getContentType();
        return type == null || type.isBlank() ? "application/octet-stream" : type;
    }

    @Override
    public void delete(Long fileId, Long operatorId, boolean admin) {
        StoredFile row = require(fileId);
        if (!admin && (operatorId == null || !operatorId.equals(row.getUploaderId()))) {
            throw BizException.forbidden("只能删除自己上传的文件");
        }
        Path path = dir.resolve(row.getStoredName()).normalize();
        try {
            if (path.startsWith(dir)) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ex) {
            throw BizException.badRequest("文件删除失败");
        }
        storedFileRepository.delete(row);
    }

    @Override
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

    private StoredFile require(Long fileId) {
        if (fileId == null) {
            throw BizException.notFound("文件不存在");
        }
        return storedFileRepository.findById(fileId).orElseThrow(() -> BizException.notFound("文件不存在"));
    }

    private FileVo toVo(StoredFile row) {
        return FileVo.builder()
                .fileId(row.getId())
                .url("/api/v1/files/" + row.getId())
                .originalName(row.getOriginalName())
                .contentType(row.getContentType())
                .sizeBytes(row.getSizeBytes())
                .purpose(row.getPurpose())
                .build();
    }

    private String normalizePurpose(String purpose) {
        if (purpose == null || purpose.isBlank()) {
            return "OTHER";
        }
        String value = purpose.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("AVATAR", "ANNOUNCEMENT", "OTHER").contains(value)) {
            throw BizException.badRequest("purpose 只能是 AVATAR、ANNOUNCEMENT 或 OTHER");
        }
        return value;
    }

    private boolean allowed(String purpose, String contentType) {
        if ("AVATAR".equals(purpose)) {
            return IMAGES.contains(contentType);
        }
        return IMAGES.contains(contentType) || DOCUMENTS.contains(contentType);
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
            default -> "";
        };
    }
}
