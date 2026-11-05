package com.camp.app.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileVo {
    private Long fileId;
    private String url;
    private String originalName;
    private String contentType;
    private Long sizeBytes;
    private String purpose;
}
