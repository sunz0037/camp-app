package com.camp.app.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 密码重置请求DTO
 */
@Data
public class ResetPasswordRequest {
    /**
     * 身份类型：1-学生，2-教工
     */
    @NotNull(message = "身份类型不能为空")
    private Integer identityType;
    
    /**
     * 学号/工号
     */
    @NotBlank(message = "学号/工号不能为空")
    private String identifier;
    
    /**
     * 真实姓名
     */
    @NotBlank(message = "真实姓名不能为空")
    private String realName;
    
    /**
     * 身份证号（完整18位）- 学生必填，教工可选
     */
    private String idCard;
    
    /**
     * 应急联系人姓名 - 仅学生
     */
    private String contactName;
    
    /**
     * 应急联系人电话 - 仅学生
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String contactPhone;
    
    /**
     * 关系：父亲、母亲、监护人、其他 - 仅学生
     */
    private String relationship;
    
    /**
     * 手机号 - 仅教工
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    
    /**
     * 验证码 - 仅教工
     */
    private String verificationCode;
}

