package com.camp.app.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 激活用户请求DTO（首次登录设置密码）
 */
@Data
public class ActivateUserRequest {
    
    /**
     * 身份类型：1-学生，2-教工
     */
    @NotNull(message = "身份类型不能为空")
    private Integer identityType;
    
    /**
     * 唯一标识：学号（学生）或工号（教工）
     */
    @NotBlank(message = "学号/工号不能为空")
    private String identifier;
    
    /**
     * 真实姓名（用于验证）
     */
    @NotBlank(message = "姓名不能为空")
    private String realName;
    
    /**
     * 新密码（至少6位）
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度不能少于6位")
    private String password;
}

