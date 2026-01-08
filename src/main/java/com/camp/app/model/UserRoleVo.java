package com.camp.app.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserRoleVo {
    private Long id;
    private Long userId;
    private Long roleId;
    private LocalDateTime createdAt;
}

