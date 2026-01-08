package com.camp.app.service;

import com.camp.app.model.ResetPasswordRequest;
import com.camp.app.model.ResetPasswordResponse;
import com.camp.app.model.SendVerificationCodeRequest;
import com.camp.app.model.SendVerificationCodeResponse;

/**
 * 密码重置服务接口
 */
public interface PasswordResetService {
    /**
     * 发送验证码（教工使用）
     * @param request 发送验证码请求
     * @param ipAddress 请求IP
     * @param userAgent 用户代理
     * @return 发送结果
     */
    SendVerificationCodeResponse sendVerificationCode(SendVerificationCodeRequest request,
                                                       String ipAddress,
                                                       String userAgent);
    
    /**
     * 重置密码
     * @param request 重置密码请求
     * @param ipAddress 请求IP
     * @param userAgent 用户代理
     * @return 重置结果
     */
    ResetPasswordResponse resetPassword(ResetPasswordRequest request,
                                        String ipAddress,
                                        String userAgent);
}

