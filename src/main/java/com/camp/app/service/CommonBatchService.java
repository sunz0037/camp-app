package com.camp.app.service;

import org.springframework.web.multipart.MultipartFile;

public interface CommonBatchService {
    void upload(String bizType, MultipartFile file);
    byte[] downloadTemplate(String bizType);
    byte[] export(String bizType, String filter);
}

