package com.camp.app.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FileVo {
    private Long fileId;
    /** 目录没有下载地址 */
    private String url;
    private String originalName;
    private String contentType;
    private Long sizeBytes;
    /** AVATAR、ANNOUNCEMENT、OTHER、STUDENT_IMPORT、STUDENT_EXPORT。目录为空 */
    private String purpose;
    private Long parentId;
    private Boolean directory;
    private Long ownerId;
    private LocalDateTime createdAt;
}
