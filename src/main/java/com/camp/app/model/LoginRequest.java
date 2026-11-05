package com.camp.app.model;

import lombok.Data;

/**
 * 用户登录请求DTO
 * 
 * 支持三类身份登录：
 * 1-学生：使用学号登录
 * 2-教工：使用工号登录
 * 3-家长/访客：仅提供按钮，不做逻辑实现
 */
@Data
public class LoginRequest {
    
    /**
     * 身份类型：1-学生，2-教工。首期统一登录可不传。家长没有独立身份。
     */
    private Integer identityType;

    /**
     * 统一登录用户名（学号或工号）。与 identifier 二选一。
     */
    private String username;
    
    /**
     * 唯一标识：学号（学生）或工号（教工）
     */
    private String identifier;
    
    /**
     * 真实姓名（用于首次登录激活时验证）
     */
    private String realName;
    
    /**
     * 密码（首次登录激活时设置，正常登录时验证）
     */
    private String password;
}
