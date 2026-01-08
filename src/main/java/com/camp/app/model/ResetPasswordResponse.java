package com.camp.app.model;

import lombok.Builder;
import lombok.Data;

/**
 * 密码重置响应DTO
 */
@Data
@Builder
public class ResetPasswordResponse {
    private Boolean success;
    private String message;
    private String maskedIdCard; // 脱敏身份证号（仅显示后4位）
    private String maskedPhone; // 脱敏手机号（仅显示后4位）- 教工使用
}

