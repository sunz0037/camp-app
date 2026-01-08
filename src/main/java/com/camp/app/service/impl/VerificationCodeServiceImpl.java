package com.camp.app.service.impl;

import com.camp.app.service.VerificationCodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 验证码服务实现类
 * 
 * 注意：当前使用内存存储验证码，适合开发测试
 * 生产环境建议使用Redis存储，支持分布式和持久化
 */
@Slf4j
@Service
public class VerificationCodeServiceImpl implements VerificationCodeService {
    
    // 验证码存储（手机号 -> 验证码）
    private final Map<String, String> codeStorage = new ConcurrentHashMap<>();
    
    // 验证令牌存储（令牌 -> 手机号）
    private final Map<String, String> tokenStorage = new ConcurrentHashMap<>();
    
    // 验证码有效期（分钟）
    private static final int CODE_EXPIRE_MINUTES = 5;
    
    // 令牌有效期（分钟）
    private static final int TOKEN_EXPIRE_MINUTES = 10;
    
    // 定时清理任务
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    public VerificationCodeServiceImpl() {
        // 启动定时清理任务（每5分钟清理一次过期数据）
        scheduler.scheduleAtFixedRate(this::cleanExpiredData, 5, 5, TimeUnit.MINUTES);
    }
    
    @Override
    public String generateAndSendCode(String phone) {
        // 生成6位数字验证码
        Random random = new Random();
        String code = String.format("%06d", random.nextInt(1000000));
        
        // 存储验证码（带时间戳，用于过期判断）
        String codeWithTime = code + "_" + System.currentTimeMillis();
        codeStorage.put(phone, codeWithTime);
        
        log.info("生成验证码: phone={}, code={}", maskPhone(phone), code);
        
        // TODO: 实际生产环境应该调用短信服务发送验证码
        // 这里仅打印日志，实际应该调用短信API
        log.info("发送验证码到手机: phone={}, code={}", maskPhone(phone), code);
        
        // 注意：生产环境不应该返回验证码，这里仅用于开发测试
        return code;
    }
    
    @Override
    public boolean verifyCode(String phone, String code) {
        String storedCodeWithTime = codeStorage.get(phone);
        if (storedCodeWithTime == null) {
            log.warn("验证码不存在: phone={}", maskPhone(phone));
            return false;
        }
        
        // 解析验证码和时间戳
        String[] parts = storedCodeWithTime.split("_");
        if (parts.length != 2) {
            log.warn("验证码格式错误: phone={}", maskPhone(phone));
            codeStorage.remove(phone);
            return false;
        }
        
        String storedCode = parts[0];
        long timestamp = Long.parseLong(parts[1]);
        
        // 检查是否过期
        long expireTime = timestamp + CODE_EXPIRE_MINUTES * 60 * 1000L;
        if (System.currentTimeMillis() > expireTime) {
            log.warn("验证码已过期: phone={}", maskPhone(phone));
            codeStorage.remove(phone);
            return false;
        }
        
        // 验证码匹配
        boolean matches = storedCode.equals(code);
        if (matches) {
            // 验证成功后删除验证码（防止重复使用）
            codeStorage.remove(phone);
            log.info("验证码验证成功: phone={}", maskPhone(phone));
        } else {
            log.warn("验证码错误: phone={}, input={}, stored={}", maskPhone(phone), code, storedCode);
        }
        
        return matches;
    }
    
    @Override
    public String generateToken(String phone) {
        // 生成随机令牌
        String token = "token_" + System.currentTimeMillis() + "_" + 
                      Long.toHexString(new Random().nextLong());
        
        // 存储令牌（带时间戳）
        String tokenWithTime = phone + "_" + System.currentTimeMillis();
        tokenStorage.put(token, tokenWithTime);
        
        log.info("生成验证令牌: phone={}, token={}", maskPhone(phone), token.substring(0, 20) + "...");
        
        return token;
    }
    
    @Override
    public String verifyToken(String token) {
        String tokenWithTime = tokenStorage.get(token);
        if (tokenWithTime == null) {
            log.warn("验证令牌不存在: token={}", token.substring(0, 20) + "...");
            return null;
        }
        
        // 解析手机号和时间戳
        String[] parts = tokenWithTime.split("_");
        if (parts.length != 2) {
            log.warn("验证令牌格式错误: token={}", token.substring(0, 20) + "...");
            tokenStorage.remove(token);
            return null;
        }
        
        String phone = parts[0];
        long timestamp = Long.parseLong(parts[1]);
        
        // 检查是否过期
        long expireTime = timestamp + TOKEN_EXPIRE_MINUTES * 60 * 1000L;
        if (System.currentTimeMillis() > expireTime) {
            log.warn("验证令牌已过期: token={}", token.substring(0, 20) + "...");
            tokenStorage.remove(token);
            return null;
        }
        
        // 验证成功后删除令牌（防止重复使用）
        tokenStorage.remove(token);
        log.info("验证令牌成功: phone={}", maskPhone(phone));
        
        return phone;
    }
    
    /**
     * 清理过期数据
     */
    private void cleanExpiredData() {
        long now = System.currentTimeMillis();
        long codeExpireTime = CODE_EXPIRE_MINUTES * 60 * 1000L;
        long tokenExpireTime = TOKEN_EXPIRE_MINUTES * 60 * 1000L;
        
        // 清理过期的验证码
        codeStorage.entrySet().removeIf(entry -> {
            String[] parts = entry.getValue().split("_");
            if (parts.length == 2) {
                long timestamp = Long.parseLong(parts[1]);
                return now - timestamp > codeExpireTime;
            }
            return true;
        });
        
        // 清理过期的令牌
        tokenStorage.entrySet().removeIf(entry -> {
            String[] parts = entry.getValue().split("_");
            if (parts.length == 2) {
                long timestamp = Long.parseLong(parts[1]);
                return now - timestamp > tokenExpireTime;
            }
            return true;
        });
    }
    
    /**
     * 脱敏手机号（仅显示后4位）
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "****";
        }
        return "****" + phone.substring(phone.length() - 4);
    }
}

