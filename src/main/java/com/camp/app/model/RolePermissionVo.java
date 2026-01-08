package com.camp.app.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RolePermissionVo {
    private Long id;
    private Long roleId;
    private Long permissionId;
    private LocalDateTime createdAt;
}

