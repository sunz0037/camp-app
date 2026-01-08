package com.camp.app.service.impl;

import com.camp.app.entity.*;
import com.camp.app.model.ResetPasswordRequest;
import com.camp.app.model.ResetPasswordResponse;
import com.camp.app.model.SendVerificationCodeRequest;
import com.camp.app.model.SendVerificationCodeResponse;
import com.camp.app.repository.*;
import com.camp.app.service.PasswordResetService;
import com.camp.app.service.VerificationCodeService;
import com.camp.app.util.PasswordEncoderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 密码重置服务实现类
 * 
 * 支持两种重置方式：
 * 1. 学生：六要素验证（学号+姓名+身份证号+应急联系人姓名+应急联系人电话+关系）
 * 2. 教工：工号+姓名+手机号，发送验证码到手机
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {
    
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final EmergencyContactRepository emergencyContactRepository;
    private final PasswordResetLogRepository passwordResetLogRepository;
    private final VerificationCodeService verificationCodeService;
    
    // 频率限制：5分钟内最多3次尝试
    private static final int MAX_ATTEMPTS = 3;
    private static final int LOCKOUT_MINUTES = 5;
    
    @Override
    @Transactional
    public SendVerificationCodeResponse sendVerificationCode(SendVerificationCodeRequest request,
                                                              String ipAddress,
                                                              String userAgent) {
        log.info("发送验证码请求: identityType={}, identifier={}, phone={}", 
                 request.getIdentityType(), request.getIdentifier(), maskPhone(request.getPhone()));
        
        // 1. 验证身份类型（仅支持教工）
        if (request.getIdentityType() == null || request.getIdentityType() != 2) {
            throw new RuntimeException("仅支持教工身份发送验证码");
        }
        
        // 2. 查找用户
        Optional<User> userOpt = userRepository.findByUsernameAndUserType(request.getIdentifier(), 2);
        if (userOpt.isEmpty()) {
            log.warn("发送验证码失败: 工号不存在 - identifier={}", request.getIdentifier());
            throw new RuntimeException("工号不存在");
        }
        
        User user = userOpt.get();
        
        // 3. 验证真实姓名
        if (!user.getRealName().equals(request.getRealName().trim())) {
            log.warn("发送验证码失败: 真实姓名不匹配 - identifier={}, provided={}, actual={}", 
                     request.getIdentifier(), request.getRealName(), user.getRealName());
            throw new RuntimeException("真实姓名不匹配");
        }
        
        // 4. 验证手机号
        if (user.getPhone() == null || !user.getPhone().equals(request.getPhone().trim())) {
            log.warn("发送验证码失败: 手机号不匹配 - identifier={}, provided={}, actual={}", 
                     request.getIdentifier(), maskPhone(request.getPhone()), maskPhone(user.getPhone()));
            throw new RuntimeException("手机号不匹配");
        }
        
        // 5. 生成并发送验证码
        verificationCodeService.generateAndSendCode(request.getPhone());
        
        // 6. 生成验证令牌
        String token = verificationCodeService.generateToken(request.getPhone());
        
        log.info("验证码发送成功: identifier={}, phone={}", 
                 request.getIdentifier(), maskPhone(request.getPhone()));
        
        return SendVerificationCodeResponse.builder()
                .success(true)
                .message("验证码已发送到手机")
                .maskedPhone(maskPhone(request.getPhone()))
                .token(token)
                .build();
    }
    
    @Override
    @Transactional
    public ResetPasswordResponse resetPassword(ResetPasswordRequest request,
                                               String ipAddress,
                                               String userAgent) {
        log.info("密码重置请求: identityType={}, identifier={}, ip={}", 
                 request.getIdentityType(), request.getIdentifier(), ipAddress);
        
        // 1. 检查频率限制
        checkAttemptLimit(request.getIdentifier());
        
        // 2. 根据身份类型处理
        if (request.getIdentityType() == 1) {
            // 学生：六要素验证
            return resetPasswordForStudent(request, ipAddress, userAgent);
        } else if (request.getIdentityType() == 2) {
            // 教工：手机验证码验证
            return resetPasswordForTeacher(request, ipAddress, userAgent);
        } else {
            throw new RuntimeException("不支持的身份类型");
        }
    }
    
    /**
     * 学生密码重置（六要素验证）
     */
    private ResetPasswordResponse resetPasswordForStudent(ResetPasswordRequest request,
                                                          String ipAddress,
                                                          String userAgent) {
        // 1. 验证必填字段
        if (request.getIdCard() == null || request.getIdCard().trim().isEmpty()) {
            logFailedAttempt(null, request, "身份证号不能为空", ipAddress, userAgent, 1);
            throw new RuntimeException("身份证号不能为空");
        }
        if (request.getContactName() == null || request.getContactName().trim().isEmpty()) {
            logFailedAttempt(null, request, "应急联系人姓名不能为空", ipAddress, userAgent, 1);
            throw new RuntimeException("应急联系人姓名不能为空");
        }
        if (request.getContactPhone() == null || request.getContactPhone().trim().isEmpty()) {
            logFailedAttempt(null, request, "应急联系人电话不能为空", ipAddress, userAgent, 1);
            throw new RuntimeException("应急联系人电话不能为空");
        }
        if (request.getRelationship() == null || request.getRelationship().trim().isEmpty()) {
            logFailedAttempt(null, request, "关系不能为空", ipAddress, userAgent, 1);
            throw new RuntimeException("关系不能为空");
        }
        
        // 2. 查找学生
        Optional<Student> studentOpt = studentRepository.findByStudentNo(request.getIdentifier());
        if (studentOpt.isEmpty()) {
            logFailedAttempt(null, request, "学号不存在", ipAddress, userAgent, 1);
            throw new RuntimeException("学号不存在");
        }
        
        Student student = studentOpt.get();
        Optional<User> userOpt = userRepository.findById(student.getUserId());
        if (userOpt.isEmpty()) {
            logFailedAttempt(null, request, "用户信息不存在", ipAddress, userAgent, 1);
            throw new RuntimeException("用户信息不存在");
        }
        if (userOpt.isEmpty()) {
            logFailedAttempt(null, request, "用户信息不存在", ipAddress, userAgent, 1);
            throw new RuntimeException("用户信息不存在");
        }
        
        User user = userOpt.get();
        
        // 3. 验证真实姓名
        if (!user.getRealName().equals(request.getRealName().trim())) {
            logFailedAttempt(user.getId(), request, "真实姓名不匹配", ipAddress, userAgent, 1);
            throw new RuntimeException("真实姓名不匹配");
        }
        
        // 4. 验证身份证号
        if (user.getIdCard() == null || !user.getIdCard().equals(request.getIdCard().trim())) {
            logFailedAttempt(user.getId(), request, "身份证号不匹配", ipAddress, userAgent, 1);
            throw new RuntimeException("身份证号不匹配");
        }
        
        // 5. 验证应急联系人信息
        Optional<EmergencyContact> contactOpt = emergencyContactRepository
                .findByStudentIdAndContactNameAndPhoneAndRelationship(
                        student.getId(),
                        request.getContactName().trim(),
                        request.getContactPhone().trim(),
                        request.getRelationship().trim()
                );
        
        if (contactOpt.isEmpty()) {
            logFailedAttempt(user.getId(), request, "应急联系人信息不匹配", ipAddress, userAgent, 1);
            throw new RuntimeException("应急联系人信息不匹配");
        }
        
        // 6. 所有验证通过，重置密码
        String newPassword = extractLast6Digits(user.getIdCard());
        String encodedPassword = PasswordEncoderUtil.encode(newPassword);
        user.setPassword(encodedPassword);
        userRepository.save(user);
        
        // 7. 记录成功日志
        logSuccessfulReset(user.getId(), request, ipAddress, userAgent, 1);
        
        log.info("学生密码重置成功: userId={}, identifier={}", user.getId(), request.getIdentifier());
        
        // 8. 返回响应
        String maskedIdCard = maskIdCard(user.getIdCard());
        
        return ResetPasswordResponse.builder()
                .success(true)
                .message("密码已重置为身份证号后6位，请使用新密码登录")
                .maskedIdCard(maskedIdCard)
                .build();
    }
    
    /**
     * 教工密码重置（手机验证码验证）
     */
    private ResetPasswordResponse resetPasswordForTeacher(ResetPasswordRequest request,
                                                          String ipAddress,
                                                          String userAgent) {
        // 1. 验证必填字段
        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            logFailedAttempt(null, request, "手机号不能为空", ipAddress, userAgent, 2);
            throw new RuntimeException("手机号不能为空");
        }
        if (request.getVerificationCode() == null || request.getVerificationCode().trim().isEmpty()) {
            logFailedAttempt(null, request, "验证码不能为空", ipAddress, userAgent, 2);
            throw new RuntimeException("验证码不能为空");
        }
        
        // 2. 查找用户
        Optional<User> userOpt = userRepository.findByUsernameAndUserType(request.getIdentifier(), 2);
        if (userOpt.isEmpty()) {
            logFailedAttempt(null, request, "工号不存在", ipAddress, userAgent, 2);
            throw new RuntimeException("工号不存在");
        }
        
        User user = userOpt.get();
        
        // 3. 验证真实姓名
        if (!user.getRealName().equals(request.getRealName().trim())) {
            logFailedAttempt(user.getId(), request, "真实姓名不匹配", ipAddress, userAgent, 2);
            throw new RuntimeException("真实姓名不匹配");
        }
        
        // 4. 验证手机号
        if (user.getPhone() == null || !user.getPhone().equals(request.getPhone().trim())) {
            logFailedAttempt(user.getId(), request, "手机号不匹配", ipAddress, userAgent, 2);
            throw new RuntimeException("手机号不匹配");
        }
        
        // 5. 验证验证码
        boolean codeValid = verificationCodeService.verifyCode(request.getPhone(), request.getVerificationCode());
        if (!codeValid) {
            logFailedAttempt(user.getId(), request, "验证码错误或已过期", ipAddress, userAgent, 2);
            throw new RuntimeException("验证码错误或已过期");
        }
        
        // 6. 验证身份证号（如果提供）
        if (request.getIdCard() != null && !request.getIdCard().trim().isEmpty()) {
            if (user.getIdCard() == null || !user.getIdCard().equals(request.getIdCard().trim())) {
                logFailedAttempt(user.getId(), request, "身份证号不匹配", ipAddress, userAgent, 2);
                throw new RuntimeException("身份证号不匹配");
            }
        }
        
        // 7. 验证身份证号是否存在
        if (user.getIdCard() == null || user.getIdCard().trim().isEmpty()) {
            logFailedAttempt(user.getId(), request, "用户未设置身份证号，无法重置密码", ipAddress, userAgent, 2);
            throw new RuntimeException("用户未设置身份证号，无法重置密码");
        }
        
        // 8. 所有验证通过，重置密码
        String newPassword = extractLast6Digits(user.getIdCard());
        String encodedPassword = PasswordEncoderUtil.encode(newPassword);
        user.setPassword(encodedPassword);
        userRepository.save(user);
        
        // 9. 记录成功日志
        logSuccessfulReset(user.getId(), request, ipAddress, userAgent, 2);
        
        log.info("教工密码重置成功: userId={}, identifier={}", user.getId(), request.getIdentifier());
        
        // 10. 返回响应
        String maskedIdCard = maskIdCard(user.getIdCard());
        String maskedPhone = maskPhone(user.getPhone());
        
        return ResetPasswordResponse.builder()
                .success(true)
                .message("密码已重置为身份证号后6位，请使用新密码登录")
                .maskedIdCard(maskedIdCard)
                .maskedPhone(maskedPhone)
                .build();
    }
    
    /**
     * 检查尝试次数限制
     */
    private void checkAttemptLimit(String identifier) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(LOCKOUT_MINUTES);
        long failedCount = passwordResetLogRepository.countFailedAttempts(identifier, since);
        
        if (failedCount >= MAX_ATTEMPTS) {
            throw new RuntimeException(
                String.format("验证失败次数过多，请%d分钟后再试", LOCKOUT_MINUTES)
            );
        }
    }
    
    /**
     * 提取身份证号后6位
     */
    private String extractLast6Digits(String idCard) {
        if (idCard == null || idCard.length() < 6) {
            throw new RuntimeException("身份证号格式不正确");
        }
        return idCard.substring(idCard.length() - 6);
    }
    
    /**
     * 脱敏身份证号（仅显示后4位）
     */
    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 4) {
            return "****";
        }
        return "****" + idCard.substring(idCard.length() - 4);
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
    
    /**
     * 记录失败日志
     */
    private void logFailedAttempt(Long userId, ResetPasswordRequest request,
                                  String reason, String ipAddress, String userAgent, Integer resetType) {
        PasswordResetLog log = new PasswordResetLog();
        log.setUserId(userId);
        log.setIdentifier(request.getIdentifier());
        log.setIdentityType(request.getIdentityType());
        log.setResetType(resetType);
        log.setStatus(0); // 失败
        log.setFailureReason(reason);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        passwordResetLogRepository.save(log);
    }
    
    /**
     * 记录成功日志
     */
    private void logSuccessfulReset(Long userId, ResetPasswordRequest request,
                                    String ipAddress, String userAgent, Integer resetType) {
        PasswordResetLog log = new PasswordResetLog();
        log.setUserId(userId);
        log.setIdentifier(request.getIdentifier());
        log.setIdentityType(request.getIdentityType());
        log.setResetType(resetType);
        log.setStatus(1); // 成功
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        passwordResetLogRepository.save(log);
    }
}

