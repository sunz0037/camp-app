package com.camp.app.service;

import com.camp.app.model.FileVo;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    FileVo upload(MultipartFile file, Long uploaderId);

    Resource load(Long fileId);

    String contentType(Long fileId);

    void delete(Long fileId, Long operatorId, boolean admin);
}
