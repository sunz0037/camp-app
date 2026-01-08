package com.camp.app.service;

import com.camp.app.model.ActivateUserRequest;
import com.camp.app.model.LoginRequest;
import com.camp.app.model.LoginResponse;

/**
 * 认证服务接口
 */
public interface AuthService {
    
    /**
     * 用户登录
     * 
     * 支持三类身份：
     * 1-学生：使用学号登录
     * 2-教工：使用工号登录
     * 3-家长/访客：不支持（仅前端显示按钮）
     * 
     * @param request 登录请求（包含身份类型、唯一标识、姓名、密码）
     * @return 登录响应（首次登录需要激活时isFirstLogin=true）
     * @throws RuntimeException 如果学号/工号与姓名不匹配，或密码错误
     */
    LoginResponse login(LoginRequest request);
    
    /**
     * 激活用户（首次登录设置密码）
     * 
     * @param request 激活请求（包含身份类型、唯一标识、姓名、新密码）
     * @return 登录响应（包含Token和用户信息）
     * @throws RuntimeException 如果学号/工号与姓名不匹配，或密码不符合要求
     */
    LoginResponse activateUser(ActivateUserRequest request);
    
    /**
     * 修改密码
     * 
     * @param userId 用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @throws RuntimeException 如果旧密码错误或新密码不符合要求
     */
    void changePassword(Long userId, String oldPassword, String newPassword);
}

