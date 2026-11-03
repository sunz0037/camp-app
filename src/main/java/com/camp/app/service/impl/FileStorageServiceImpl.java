package com.camp.app.service.impl;

import com.camp.app.api.BizException;
import com.camp.app.entity.StoredFile;
import com.camp.app.model.FileVo;
import com.camp.app.repository.StoredFileRepository;
import com.camp.app.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {
    private final StoredFileRepository storedFileRepository;

    @Value("${camp.upload.dir:data/uploads}")
    private String uploadDir;

    @Override
    @Transactional
    public FileVo upload(MultipartFile file, Long uploaderId) {
        if (uploaderId == null) {
            throw BizException.unauthorized("未登录");
        }
        if (file == null || file.isEmpty()) {
            throw BizException.badRequest("文件不能为空");
        }
        try {
            Path dir = Path.of(uploadDir);
            Files.createDirectories(dir);
            String storedName = UUID.randomUUID() + "-" + sanitize(file.getOriginalFilename());
            Path target = dir.resolve(storedName);
            file.transferTo(target);
            StoredFile row = new StoredFile();
            row.setOriginalName(file.getOriginalFilename() == null ? storedName : file.getOriginalFilename());
            row.setStoredName(storedName);
            row.setContentType(file.getContentType());
            row.setSizeBytes(file.getSize());
            row.setUploaderId(uploaderId);
            storedFileRepository.save(row);
            return FileVo.builder()
                    .fileId(row.getId())
                    .url("/api/v1/files/" + row.getId())
                    .originalName(row.getOriginalName())
                    .build();
        } catch (IOException ex) {
            throw BizException.badRequest("文件保存失败");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Resource load(Long fileId) {
        StoredFile row = storedFileRepository.findById(fileId)
                .orElseThrow(() -> BizException.notFound("文件不存在"));
        Path path = Path.of(uploadDir).resolve(row.getStoredName());
        if (!Files.exists(path)) {
            throw BizException.notFound("文件不存在");
        }
        return new FileSystemResource(path);
    }

    @Override
    @Transactional(readOnly = true)
    public String contentType(Long fileId) {
        return storedFileRepository.findById(fileId).map(StoredFile::getContentType).orElse("application/octet-stream");
    }

    @Override
    @Transactional
    public void delete(Long fileId, Long operatorId, boolean admin) {
        StoredFile row = storedFileRepository.findById(fileId)
                .orElseThrow(() -> BizException.notFound("文件不存在"));
        if (!admin && (operatorId == null || !operatorId.equals(row.getUploaderId()))) {
            throw BizException.forbidden("只能删除自己上传的文件");
        }
        try {
            Files.deleteIfExists(Path.of(uploadDir).resolve(row.getStoredName()));
        } catch (IOException ignored) {
            // 元数据仍删除，避免悬挂记录
        }
        storedFileRepository.delete(row);
    }

    private String sanitize(String name) {
        if (name == null || name.isBlank()) {
            return "file";
        }
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
