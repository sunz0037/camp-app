package com.camp.app.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RoleVo {
    private Long id;
    private String roleCode;
    private String roleName;
    private String description;
    /**
     * 0-禁用，1-启用
     */
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

