package com.camp.app.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PermissionVo {
    private Long id;
    private String permissionCode;
    private String permissionName;
    private String resourceType;
    private String resourcePath;
    private Long parentId;
    private String description;
    private Integer sortOrder;
    /**
     * 0-禁用，1-启用
     */
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private java.util.List<PermissionVo> children;
}

