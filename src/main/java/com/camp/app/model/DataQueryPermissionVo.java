package com.camp.app.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DataQueryPermissionVo {
    private Long id;
    private Long userId;
    private Long roleId;
    private String dataType;
    private String queryScope;
    private String scopeValue;
    private String fieldPermissions;
    private Integer canExport;
    private Integer canPrint;
    /**
     * 0-禁用，1-启用
     */
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

