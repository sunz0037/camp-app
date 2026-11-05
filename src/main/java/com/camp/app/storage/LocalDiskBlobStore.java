package com.camp.app.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 把对象写到 camp.upload.dir。对象名只允许单层文件名，避免路径跳出目录。
 */
@Component
@ConditionalOnProperty(name = "camp.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalDiskBlobStore implements BlobStore {
    private final Path dir;

    public LocalDiskBlobStore(@Value("${camp.upload.dir:data/uploads}") String uploadDir) {
        this.dir = Path.of(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.dir);
        } catch (IOException ex) {
            throw new IllegalStateException("无法准备上传目录 " + this.dir, ex);
        }
    }

    @Override
    public void write(String objectName, InputStream input) throws IOException {
        Path target = resolve(objectName);
        Files.copy(input, target);
    }

    @Override
    public Resource read(String objectName) {
        return new FileSystemResource(resolve(objectName));
    }

    @Override
    public boolean exists(String objectName) {
        return Files.exists(resolve(objectName));
    }

    @Override
    public void delete(String objectName) throws IOException {
        Files.deleteIfExists(resolve(objectName));
    }

    private Path resolve(String objectName) {
        if (objectName == null || objectName.isBlank()
                || objectName.contains("/") || objectName.contains("\\") || objectName.contains("..")) {
            throw new IllegalArgumentException("对象名不合法");
        }
        Path path = dir.resolve(objectName).normalize();
        if (!path.startsWith(dir)) {
            throw new IllegalArgumentException("对象名不合法");
        }
        return path;
    }
}
