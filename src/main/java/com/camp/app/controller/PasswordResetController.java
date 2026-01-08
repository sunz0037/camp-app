package com.camp.app.controller;

import com.camp.app.api.BaseResponse;
import com.camp.app.model.ResetPasswordRequest;
import com.camp.app.model.ResetPasswordResponse;
import com.camp.app.model.SendVerificationCodeRequest;
import com.camp.app.model.SendVerificationCodeResponse;
import com.camp.app.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 密码重置控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PasswordResetController {
    
    private final PasswordResetService passwordResetService;
    
    /**
     * 发送验证码（教工使用）
     */
    @PostMapping("/send-verification-code")
    public BaseResponse<SendVerificationCodeResponse> sendVerificationCode(
            @Valid @RequestBody SendVerificationCodeRequest request,
            HttpServletRequest httpRequest) {
        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            
            SendVerificationCodeResponse response = passwordResetService.sendVerificationCode(
                request, ipAddress, userAgent
            );
            
            return BaseResponse.ok(response);
        } catch (RuntimeException e) {
            log.warn("发送验证码失败: {}", e.getMessage());
            return BaseResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("发送验证码异常", e);
            return BaseResponse.error(500, "系统错误，请稍后重试");
        }
    }
    
    /**
     * 重置密码
     */
    @PostMapping("/reset-password")
    public BaseResponse<ResetPasswordResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest) {
        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            
            ResetPasswordResponse response = passwordResetService.resetPassword(
                request, ipAddress, userAgent
            );
            
            return BaseResponse.ok(response);
        } catch (RuntimeException e) {
            log.warn("密码重置失败: {}", e.getMessage());
            return BaseResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("密码重置异常", e);
            return BaseResponse.error(500, "系统错误，请稍后重试");
        }
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}

