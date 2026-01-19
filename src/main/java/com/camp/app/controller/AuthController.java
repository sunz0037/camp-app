package com.camp.app.controller;

import com.camp.app.api.BaseResponse;
import com.camp.app.model.ActivateUserRequest;
import com.camp.app.model.LoginRequest;
import com.camp.app.model.LoginResponse;
import com.camp.app.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * 处理用户登录、激活、登出、密码修改等认证相关功能
 * 
 * 支持三类身份登录：
 * 1-学生：使用学号登录
 * 2-教工：使用工号登录
 * 3-家长/访客：仅提供按钮，不做逻辑实现
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    
    /**
     * 用户登录
     * 
     * 支持学生和教工登录：
     * - 学生：使用学号 + 姓名（首次登录）或 学号 + 密码（正常登录）
     * - 教工：使用工号 + 姓名（首次登录）或 工号 + 密码（正常登录）
     * 
     * @param request 登录请求（身份类型、唯一标识、姓名、密码）
     * @return 登录响应（首次登录时isFirstLogin=true，需要调用激活接口）
     */
    @PostMapping("/login")
    public BaseResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("收到登录请求: identityType={}, identifier={}", request.getIdentityType(), request.getIdentifier());
        
        try {
            LoginResponse response = authService.login(request);
            return BaseResponse.ok(response);
        } catch (RuntimeException e) {
            log.error("登录失败: {}", e.getMessage());
            return BaseResponse.error(401, e.getMessage());
        }
    }
    
    /**
     * 激活用户（首次登录设置密码）
     * 
     * 用于学生和教工的首次登录激活：
     * - 验证学号/工号与姓名是否匹配
     * - 设置密码（至少6位）
     * - 激活后自动登录
     * 
     * @param request 激活请求（身份类型、唯一标识、姓名、新密码）
     * @return 登录响应（包含Token和用户信息）
     */
    @PostMapping("/activate")
    public BaseResponse<LoginResponse> activateUser(@Valid @RequestBody ActivateUserRequest request) {
        log.info("收到激活用户请求: identityType={}, identifier={}", request.getIdentityType(), request.getIdentifier());
        
        try {
            LoginResponse response = authService.activateUser(request);
            return BaseResponse.ok(response);
        } catch (RuntimeException e) {
            log.error("激活用户失败: {}", e.getMessage());
            return BaseResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 修改密码
     * 
     * @param userId 用户ID（从Token中获取，暂时使用路径参数）
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 操作结果
     */
    @PutMapping("/password")
    public BaseResponse<Void> changePassword(
            @RequestParam Long userId,
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {
        log.info("收到修改密码请求: userId={}", userId);
        
        try {
            authService.changePassword(userId, oldPassword, newPassword);
            return BaseResponse.ok(null);
        } catch (RuntimeException e) {
            log.error("修改密码失败: {}", e.getMessage());
            return BaseResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 用户登出（暂时返回成功，后续实现Token黑名单）
     */
    @PostMapping("/logout")
    public BaseResponse<Void> logout() {
        log.info("用户登出");
        // TODO: 实现Token黑名单机制
        return BaseResponse.ok(null);
    }
    
    /**
     * 刷新Token（暂时返回成功，后续实现JWT刷新机制）
     */
    @PostMapping("/refresh")
    public BaseResponse<LoginResponse> refreshToken(@RequestParam String refreshToken) {
        log.info("刷新Token请求");
        // TODO: 实现JWT Token刷新机制
        return BaseResponse.error(501, "功能暂未实现");
    }
    
    /**
     * 获取当前用户信息
     * 
     * @param userId 用户ID（从请求头 X-User-Id 获取）
     * @return 当前用户信息
     */
    @GetMapping("/me")
    public BaseResponse<LoginResponse.UserInfo> getCurrentUser(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        log.info("获取当前用户信息请求: userId={}", userId);
        
        try {
            LoginResponse.UserInfo userInfo = authService.getCurrentUser(userId);
            return BaseResponse.ok(userInfo);
        } catch (RuntimeException e) {
            log.error("获取当前用户信息失败: {}", e.getMessage());
            return BaseResponse.error(400, e.getMessage());
        }
    }
}
