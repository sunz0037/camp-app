package com.camp.app.service;

/**
 * 验证码服务接口
 */
public interface VerificationCodeService {
    /**
     * 生成并发送验证码
     * @param phone 手机号
     * @return 验证码（用于测试，生产环境不应返回）
     */
    String generateAndSendCode(String phone);
    
    /**
     * 验证验证码
     * @param phone 手机号
     * @param code 验证码
     * @return true-验证通过，false-验证失败
     */
    boolean verifyCode(String phone, String code);
    
    /**
     * 生成验证令牌（用于关联验证码和用户）
     * @param phone 手机号
     * @return 验证令牌
     */
    String generateToken(String phone);
    
    /**
     * 验证令牌
     * @param token 验证令牌
     * @return 手机号，如果令牌无效返回null
     */
    String verifyToken(String token);
}

