package com.camp.app.service;

import com.camp.app.api.PageResult;
import com.camp.app.model.FileVo;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    PageResult<FileVo> list(long page, long size, Long parentId, String purpose, String scope);

    default FileVo upload(MultipartFile file, Long uploaderId, String purpose) {
        return upload(file, uploaderId, purpose, null);
    }

    FileVo upload(MultipartFile file, Long uploaderId, String purpose, Long parentId);

    FileVo createFolder(String name, Long parentId);

    /** 把已经生成的字节存成文件，学生导出走这里。 */
    FileVo storeBytes(Long ownerId, String purpose, String originalName, String contentType, byte[] body);

    /** 读回文本。学生导入只接受调用者能读的 CSV / 纯文本。 */
    String readUtf8(Long fileId);

    Resource load(Long fileId);

    String contentType(Long fileId);

    String downloadName(Long fileId);

    void delete(Long fileId, Long operatorId, boolean admin);

    /**
     * 头像或公告附件若指向本服务的 /api/v1/files/{id}，则要求记录存在。
     * 外部 http(s) 地址保持原样，不在这里拉取。
     */
    void assertReferenced(String url);
}