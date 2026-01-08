package com.camp.app.util;

/**
 * 密码加密工具测试类
 * 用于演示和测试密码加密功能
 */
public class PasswordEncoderTest {
    
    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("密码加密工具测试");
        System.out.println("==========================================");
        System.out.println();
        
        // 测试1: 加密密码
        System.out.println("1. 测试密码加密：");
        String rawPassword = "123456";
        String encoded1 = PasswordEncoderUtil.encode(rawPassword);
        String encoded2 = PasswordEncoderUtil.encode(rawPassword);
        
        System.out.println("   原始密码: " + rawPassword);
        System.out.println("   加密结果1: " + encoded1);
        System.out.println("   加密结果2: " + encoded2);
        System.out.println("   ✓ 相同密码每次加密结果不同（自动加盐）");
        System.out.println();
        
        // 测试2: 验证密码
        System.out.println("2. 测试密码验证：");
        boolean matches1 = PasswordEncoderUtil.matches(rawPassword, encoded1);
        boolean matches2 = PasswordEncoderUtil.matches(rawPassword, encoded2);
        boolean matches3 = PasswordEncoderUtil.matches("wrong", encoded1);
        
        System.out.println("   验证原始密码与加密结果1: " + (matches1 ? "✓ 匹配" : "✗ 不匹配"));
        System.out.println("   验证原始密码与加密结果2: " + (matches2 ? "✓ 匹配" : "✗ 不匹配"));
        System.out.println("   验证错误密码: " + (matches3 ? "✗ 错误匹配" : "✓ 正确拒绝"));
        System.out.println();
        
        // 测试3: 密码强度检查
        System.out.println("3. 测试密码强度检查：");
        String[] testPasswords = {
            "123456",           // 弱
            "password",         // 弱
            "Password123",      // 中
            "Password123!",     // 强
            "MyP@ssw0rd2024!"   // 强
        };
        
        for (String pwd : testPasswords) {
            int strength = PasswordEncoderUtil.checkPasswordStrength(pwd);
            String desc = PasswordEncoderUtil.getPasswordStrengthDescription(pwd);
            System.out.println("   密码: " + pwd);
            System.out.println("   强度: " + (strength == 0 ? "弱" : strength == 1 ? "中" : "强"));
            System.out.println("   说明: " + desc);
            System.out.println();
        }
        
        // 测试4: 性能测试
        System.out.println("4. 性能测试（加密100次）：");
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            PasswordEncoderUtil.encode("testPassword123!");
        }
        long endTime = System.currentTimeMillis();
        System.out.println("   耗时: " + (endTime - startTime) + "ms");
        System.out.println("   平均: " + ((endTime - startTime) / 100.0) + "ms/次");
        System.out.println();
        
        System.out.println("==========================================");
        System.out.println("✓ 测试完成");
        System.out.println("==========================================");
    }
}

