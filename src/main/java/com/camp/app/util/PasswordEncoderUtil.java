package com.camp.app.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码加密工具类
 * 
 * 使用BCrypt算法进行密码加密和验证
 * 
 * BCrypt特点：
 * 1. 自动生成随机盐值，每次加密结果不同
 * 2. 单向哈希，无法逆向解密
 * 3. 可调整计算强度，平衡安全性和性能
 * 4. 广泛使用，经过充分验证
 * 
 * @author camp-app
 */
public class PasswordEncoderUtil {
    
    /**
     * BCrypt强度配置
     * - 5: 较低强度，适合开发测试环境（快速）
     * - 10: 默认强度，适合大多数应用（推荐）
     * - 12: 更高安全性，但计算稍慢
     * - 不建议超过12，会影响性能
     */
    private static final int BCRYPT_STRENGTH = 5;
    
    /**
     * 密码编码器实例（单例模式）
     */
    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    
    /**
     * 加密密码
     * 
     * @param rawPassword 明文密码
     * @return 加密后的密码（BCrypt格式，60字符）
     * 
     * @example
     * String encoded = PasswordEncoderUtil.encode("123456");
     * // 输出: $2a$05$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
     */
    public static String encode(String rawPassword) {
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        return passwordEncoder.encode(rawPassword);
    }
    
    /**
     * 验证密码是否匹配
     * 
     * @param rawPassword 用户输入的明文密码
     * @param encodedPassword 数据库中存储的加密密码
     * @return true-密码匹配，false-密码不匹配
     * 
     * @example
     * boolean matches = PasswordEncoderUtil.matches("123456", user.getPassword());
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
    
    /**
     * 检查密码是否需要重新加密
     * 用于密码强度升级场景
     * 
     * @param encodedPassword 已加密的密码
     * @return true-需要重新加密（强度不足），false-不需要
     */
    public static boolean needsUpgrade(String encodedPassword) {
        if (encodedPassword == null || !encodedPassword.startsWith("$2a$")) {
            return true; // 不是BCrypt格式，需要升级
        }
        
        // 提取强度值（BCrypt格式：$2a$strength$...）
        try {
            String strengthStr = encodedPassword.substring(4, 6);
            int strength = Integer.parseInt(strengthStr);
            return strength < BCRYPT_STRENGTH;
        } catch (Exception e) {
            return true; // 格式错误，需要升级
        }
    }
    
    /**
     * 验证密码强度
     * 
     * @param password 明文密码
     * @return 密码强度等级：0-弱，1-中，2-强
     */
    public static int checkPasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            return 0; // 弱
        }
        
        int strength = 0;
        
        // 长度检查
        if (password.length() >= 12) {
            strength++;
        }
        
        // 包含小写字母
        if (password.matches(".*[a-z].*")) {
            strength++;
        }
        
        // 包含大写字母
        if (password.matches(".*[A-Z].*")) {
            strength++;
        }
        
        // 包含数字
        if (password.matches(".*[0-9].*")) {
            strength++;
        }
        
        // 包含特殊字符
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            strength++;
        }
        
        // 评分：0-2个条件=弱，3-4个条件=中，5个条件=强
        if (strength <= 2) {
            return 0; // 弱
        } else if (strength <= 4) {
            return 1; // 中
        } else {
            return 2; // 强
        }
    }
    
    /**
     * 获取密码强度描述
     * 
     * @param password 明文密码
     * @return 密码强度描述
     */
    public static String getPasswordStrengthDescription(String password) {
        int strength = checkPasswordStrength(password);
        switch (strength) {
            case 0:
                return "弱密码：建议至少8位，包含大小写字母、数字和特殊字符";
            case 1:
                return "中等强度：建议增加长度或添加特殊字符";
            case 2:
                return "强密码";
            default:
                return "未知";
        }
    }
}

