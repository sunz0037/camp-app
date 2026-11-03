package com.camp.app.model;

import lombok.Data;

/**
 * 二期最小找回。学生走六要素，教工走手机号 + 开发态固定验证码。
 */
@Data
public class Phase2ResetPasswordRequest {
    /** 1 学生，2 教工 */
    private Integer identityType;
    /** 学生学号，或教工登录名 */
    private String identifier;
    private String realName;
    private String idCard;
    private String contactName;
    private String contactPhone;
    private String relationship;
    private String phone;
    private String verificationCode;
    private String newPassword;
}
