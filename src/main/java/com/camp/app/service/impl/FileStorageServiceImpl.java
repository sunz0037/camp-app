package com.camp.app.service.impl;

import com.camp.app.api.BizException;
import com.camp.app.model.FileVo;
import com.camp.app.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 首期库没有文件表。元数据写在上传目录的 index.tsv，避免 JPA 另建表。
 */
@Service
public class FileStorageServiceImpl implements FileStorageService {
    private final Path dir;
    private final Path index;
    private final AtomicLong sequence = new AtomicLong(1);

    public FileStorageServiceImpl(@Value("${camp.upload.dir:data/uploads}") String uploadDir) {
        this.dir = Path.of(uploadDir);
        this.index = dir.resolve("index.tsv");
        try {
            Files.createDirectories(dir);
            if (Files.exists(index)) {
                for (String line : Files.readAllLines(index)) {
                    String[] cols = line.split("\t", -1);
                    if (cols.length > 0 && !cols[0].isBlank()) {
                        sequence.updateAndGet(current -> Math.max(current, Long.parseLong(cols[0]) + 1));
                    }
                }
            }
        } catch (IOException | NumberFormatException ex) {
            throw new IllegalStateException("无法准备上传目录", ex);
        }
    }

    @Override
    public synchronized FileVo upload(MultipartFile file, Long uploaderId) {
        if (uploaderId == null) {
            throw BizException.unauthorized("未登录");
        }
        if (file == null || file.isEmpty()) {
            throw BizException.badRequest("文件不能为空");
        }
        try {
            long id = sequence.getAndIncrement();
            String storedName = id + "-" + sanitize(file.getOriginalFilename());
            file.transferTo(dir.resolve(storedName));
            String original = file.getOriginalFilename() == null ? storedName : file.getOriginalFilename().replace('\t', ' ');
            String contentType = file.getContentType() == null ? "" : file.getContentType();
            String line = id + "\t" + storedName + "\t" + original + "\t" + contentType + "\t" + uploaderId + "\n";
            Files.writeString(index, line, StandardCharsets.UTF_8, Files.exists(index)
                    ? java.nio.file.StandardOpenOption.APPEND
                    : java.nio.file.StandardOpenOption.CREATE);
            return FileVo.builder().fileId(id).url("/api/v1/files/" + id).originalName(original).build();
        } catch (IOException ex) {
            throw BizException.badRequest("文件保存失败");
        }
    }

    @Override
    public Resource load(Long fileId) {
        String[] row = find(fileId);
        Path path = dir.resolve(row[1]);
        if (!Files.exists(path)) {
            throw BizException.notFound("文件不存在");
        }
        return new FileSystemResource(path);
    }

    @Override
    public String contentType(Long fileId) {
        String type = find(fileId)[3];
        return type == null || type.isBlank() ? "application/octet-stream" : type;
    }

    @Override
    public synchronized void delete(Long fileId, Long operatorId, boolean admin) {
        String[] row = find(fileId);
        long owner = Long.parseLong(row[4]);
        if (!admin && (operatorId == null || owner != operatorId)) {
            throw BizException.forbidden("只能删除自己上传的文件");
        }
        try {
            Files.deleteIfExists(dir.resolve(row[1]));
            List<String> lines = Files.readAllLines(index).stream()
                    .filter(line -> !line.startsWith(fileId + "\t"))
                    .toList();
            Files.write(index, lines);
        } catch (IOException ex) {
            throw BizException.badRequest("文件删除失败");
        }
    }

    private String[] find(Long fileId) {
        try {
            if (!Files.exists(index)) {
                throw BizException.notFound("文件不存在");
            }
            for (String line : Files.readAllLines(index)) {
                String[] cols = line.split("\t", -1);
                if (cols.length >= 5 && cols[0].equals(String.valueOf(fileId))) {
                    return cols;
                }
            }
        } catch (IOException ex) {
            throw BizException.notFound("文件不存在");
        }
        throw BizException.notFound("文件不存在");
    }

    private String sanitize(String name) {
        if (name == null || name.isBlank()) {
            return "file";
        }
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
