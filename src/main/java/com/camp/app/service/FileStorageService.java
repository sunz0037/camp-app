package com.camp.app.service;

import com.camp.app.model.FileVo;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    FileVo upload(MultipartFile file, Long uploaderId, String purpose);

    Resource load(Long fileId);

    String contentType(Long fileId);

    void delete(Long fileId, Long operatorId, boolean admin);

    /**
     * 头像或公告附件若指向本服务的 /api/v1/files/{id}，则要求记录存在。
     * 外部 http(s) 地址保持原样，不在这里拉取。
     */
    void assertReferenced(String url);
}
