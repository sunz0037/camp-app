package com.camp.app.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

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
    /** 创建或编辑时可选。缺省按 userType 映射 STUDENT/TEACHER/ADMIN。初始密码只在创建时生效。 */
    private List<String> roleCodes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

