package com.camp.app.model;

import lombok.Data;

/**
 * 激活用户请求DTO（首次登录设置密码）
 */
@Data
public class ActivateUserRequest {
    
    /**
     * 身份类型：1-学生，2-教工。已登录改密时可不传。
     */
    private Integer identityType;
    
    /**
     * 唯一标识：学号（学生）或工号（教工）。已登录改密时可不传。
     */
    private String identifier;
    
    /**
     * 真实姓名（未登录的旧激活流程用于验证）
     */
    private String realName;
    
    /**
     * 新密码（至少6位）
     */
    private String password;

    /**
     * 与 password 同义，便于契约示例。
     */
    private String newPassword;

    /**
     * 可选绑定手机。
     */
    private String phone;
}

