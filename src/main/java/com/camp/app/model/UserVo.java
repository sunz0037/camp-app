package com.camp.app.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVo {
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String idCard;
    private String phone;
    private String email;
    /**
     * 1-学生，2-教职工，3-管理员
     */
    private Integer userType;
    /**
     * 0-禁用，1-启用
     */
    private Integer status;
    private String avatarUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

