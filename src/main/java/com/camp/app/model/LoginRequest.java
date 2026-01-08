package com.camp.app.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
     * 身份类型：1-学生，2-教工，3-家长/访客
     */
    @NotNull(message = "身份类型不能为空")
    private Integer identityType;
    
    /**
     * 唯一标识：学号（学生）或工号（教工）
     */
    @NotBlank(message = "学号/工号不能为空")
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
