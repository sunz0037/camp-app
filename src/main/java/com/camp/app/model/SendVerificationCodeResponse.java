package com.camp.app.model;

import lombok.Builder;
import lombok.Data;

/**
 * 发送验证码响应DTO
 */
@Data
@Builder
public class SendVerificationCodeResponse {
    private Boolean success;
    private String message;
    private String maskedPhone; // 脱敏手机号（仅显示后4位）
    private String token; // 验证令牌（用于后续验证）
}

