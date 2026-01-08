package com.camp.app.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 发送验证码请求DTO（教工使用）
 */
@Data
public class SendVerificationCodeRequest {
    /**
     * 身份类型：2-教工
     */
    @NotNull(message = "身份类型不能为空")
    private Integer identityType;
    
    /**
     * 工号
     */
    @NotBlank(message = "工号不能为空")
    private String identifier;
    
    /**
     * 真实姓名
     */
    @NotBlank(message = "真实姓名不能为空")
    private String realName;
    
    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}

