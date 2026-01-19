package com.camp.app.service.impl;

import com.camp.app.entity.Student;
import com.camp.app.entity.User;
import com.camp.app.model.ActivateUserRequest;
import com.camp.app.model.LoginRequest;
import com.camp.app.model.LoginResponse;
import com.camp.app.repository.StudentRepository;
import com.camp.app.repository.UserRepository;
import com.camp.app.service.AuthService;
import com.camp.app.util.PasswordEncoderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 认证服务实现类
 * 使用BCrypt进行密码加密和验证
 * 支持三类身份登录：1-学生，2-教工，3-家长/访客
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    
    // 默认密码标识（用于判断是否首次登录）
    private static final String DEFAULT_PASSWORD_PLACEHOLDER = "DEFAULT_PASSWORD_NOT_SET";
    
    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        log.info("用户登录尝试: identityType={}, identifier={}", request.getIdentityType(), request.getIdentifier());
        
        // 1. 验证身份类型
        if (request.getIdentityType() == null) {
            throw new RuntimeException("身份类型不能为空");
        }
        
        // 2. 家长/访客身份不支持登录
        if (request.getIdentityType() == 3) {
            throw new RuntimeException("家长/访客身份登录功能暂未实现");
        }
        
        User user = null;
        
        // 3. 根据身份类型查找用户
        if (request.getIdentityType() == 1) {
            // 学生：通过学号查找
            user = findStudentByStudentNo(request.getIdentifier());
            if (user == null) {
                log.warn("登录失败: 学号不存在 - studentNo={}", request.getIdentifier());
                throw new RuntimeException("学号不存在");
            }
        } else if (request.getIdentityType() == 2) {
            // 教工：通过工号（username）查找
            Optional<User> userOpt = userRepository.findByUsernameAndUserType(request.getIdentifier(), 2);
            if (userOpt.isEmpty()) {
                log.warn("登录失败: 工号不存在 - employeeNo={}", request.getIdentifier());
                throw new RuntimeException("工号不存在");
            }
            user = userOpt.get();
        } else {
            throw new RuntimeException("不支持的身份类型");
        }
        
        // 4. 检查用户状态
        if (user.getStatus() == null || user.getStatus() == 0) {
            log.warn("登录失败: 用户已禁用 - userId={}", user.getId());
            throw new RuntimeException("用户已被禁用，请联系管理员");
        }
        
        // 5. 判断是否首次登录（密码未设置）
        // 优先判断是否首次登录，避免直接提示密码不能为空
        String encodedPassword = user.getPassword();
        boolean isFirstLoginUser = (encodedPassword == null || encodedPassword.isEmpty() || 
            encodedPassword.equals(DEFAULT_PASSWORD_PLACEHOLDER) ||
            isDefaultPassword(encodedPassword));
        
        if (isFirstLoginUser) {
            // 首次登录，需要验证姓名并激活
            
            // 验证姓名（必须提供）
            if (request.getRealName() == null || request.getRealName().trim().isEmpty()) {
                String identifierName = request.getIdentityType() == 1 ? "学号" : "工号";
                throw new RuntimeException(String.format("首次登录需要提供姓名进行验证，请输入%s对应的姓名", identifierName));
            }
            
            // 验证姓名是否匹配
            if (!user.getRealName().equals(request.getRealName().trim())) {
                String errorMsg = request.getIdentityType() == 1 ? 
                    "学号与姓名不匹配，请确认" : "工号与姓名不匹配，请确认";
                log.warn("登录失败: {} - identifier={}, providedName={}, actualName={}", 
                    errorMsg, request.getIdentifier(), request.getRealName(), user.getRealName());
                throw new RuntimeException(errorMsg);
            }
            
            // 姓名验证成功，返回首次登录响应，提示设置密码
            String identifierName = request.getIdentityType() == 1 ? "学号" : "工号";
            log.info("首次登录验证成功: {}={}, 姓名={}", identifierName, request.getIdentifier(), user.getRealName());
            
            return LoginResponse.builder()
                    .isFirstLogin(true)
                    .message(String.format("该%s对应用户信息已激活，请设置密码（密码长度不少于6位）", identifierName))
                    .userInfo(LoginResponse.UserInfo.builder()
                            .id(user.getId())
                            .username(user.getUsername())
                            .realName(user.getRealName())
                            .userType(user.getUserType())
                            .avatarUrl(user.getAvatarUrl())
                            .build())
                    .build();
        }
        
        // 6. 正常登录：验证密码（只有已激活用户才需要密码）
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new RuntimeException("密码不能为空");
        }
        
        boolean passwordMatches = PasswordEncoderUtil.matches(request.getPassword(), encodedPassword);
        if (!passwordMatches) {
            log.warn("登录失败: 密码错误 - userId={}", user.getId());
            throw new RuntimeException("密码错误");
        }
        
        // 7. 登录成功，构建响应
        log.info("登录成功: userId={}, identityType={}", user.getId(), request.getIdentityType());
        
        // TODO: 生成JWT Token（暂时返回固定值）
        String token = "mock-jwt-token-" + user.getId();
        String refreshToken = "mock-refresh-token-" + user.getId();
        
        LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .userType(user.getUserType())
                .avatarUrl(user.getAvatarUrl())
                .build();
        
        return LoginResponse.builder()
                .isFirstLogin(false)
                .token(token)
                .refreshToken(refreshToken)
                .userInfo(userInfo)
                .build();
    }
    
    @Override
    @Transactional
    public LoginResponse activateUser(ActivateUserRequest request) {
        log.info("激活用户: identityType={}, identifier={}", request.getIdentityType(), request.getIdentifier());
        
        // 1. 验证身份类型
        if (request.getIdentityType() == null || (request.getIdentityType() != 1 && request.getIdentityType() != 2)) {
            throw new RuntimeException("身份类型错误，仅支持学生和教工");
        }
        
        User user = null;
        
        // 2. 根据身份类型查找用户
        if (request.getIdentityType() == 1) {
            // 学生：通过学号查找
            user = findStudentByStudentNo(request.getIdentifier());
            if (user == null) {
                throw new RuntimeException("学号不存在");
            }
        } else if (request.getIdentityType() == 2) {
            // 教工：通过工号查找
            Optional<User> userOpt = userRepository.findByUsernameAndUserType(request.getIdentifier(), 2);
            if (userOpt.isEmpty()) {
                throw new RuntimeException("工号不存在");
            }
            user = userOpt.get();
        } else {
            throw new RuntimeException("不支持的身份类型");
        }
        
        // 3. 验证用户是否存在
        if (user == null) {
            String errorMsg = request.getIdentityType() == 1 ? 
                "学号不存在" : "工号不存在";
            throw new RuntimeException(errorMsg);
        }
        
        // 4. 验证姓名
        if (!user.getRealName().equals(request.getRealName().trim())) {
            String errorMsg = request.getIdentityType() == 1 ? 
                "学号与姓名不匹配，请确认" : "工号与姓名不匹配，请确认";
            log.warn("激活失败: {} - identifier={}, providedName={}, actualName={}", 
                errorMsg, request.getIdentifier(), request.getRealName(), user.getRealName());
            throw new RuntimeException(errorMsg);
        }
        
        // 5. 验证密码长度（至少6位）
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new RuntimeException("密码长度不能少于6位");
        }
        
        // 6. 使用BCrypt加密密码
        String encodedPassword = PasswordEncoderUtil.encode(request.getPassword());
        user.setPassword(encodedPassword);
        
        // 7. 保存到数据库
        userRepository.save(user);
        
        log.info("用户激活成功: userId={}", user.getId());
        
        // 8. 返回登录响应
        String token = "mock-jwt-token-" + user.getId();
        String refreshToken = "mock-refresh-token-" + user.getId();
        
        LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .userType(user.getUserType())
                .avatarUrl(user.getAvatarUrl())
                .build();
        
        return LoginResponse.builder()
                .isFirstLogin(false)
                .token(token)
                .refreshToken(refreshToken)
                .userInfo(userInfo)
                .message("激活成功，已设置密码")
                .build();
    }
    
    @Override
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        log.info("修改密码: userId={}", userId);
        
        // 1. 查询用户
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 2. 验证旧密码
        boolean oldPasswordMatches = PasswordEncoderUtil.matches(oldPassword, user.getPassword());
        if (!oldPasswordMatches) {
            log.warn("修改密码失败: 旧密码错误 - userId={}", userId);
            throw new RuntimeException("旧密码错误");
        }
        
        // 3. 验证新密码长度（至少6位）
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("新密码长度不能少于6位");
        }
        
        // 4. 使用BCrypt加密新密码
        String encodedNewPassword = PasswordEncoderUtil.encode(newPassword);
        user.setPassword(encodedNewPassword);
        
        // 5. 保存到数据库
        userRepository.save(user);
        
        log.info("密码修改成功: userId={}", userId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public LoginResponse.UserInfo getCurrentUser(Long userId) {
        log.info("获取当前用户信息: userId={}", userId);
        
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空，请先登录");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        return LoginResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .userType(user.getUserType())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
    
    /**
     * 通过学号查找学生对应的用户
     */
    private User findStudentByStudentNo(String studentNo) {
        Optional<Student> studentOpt = studentRepository.findByStudentNo(studentNo);
        if (studentOpt.isEmpty()) {
            return null;
        }
        
        Student student = studentOpt.get();
        if (student.getUserId() == null) {
            return null;
        }
        return userRepository.findById(student.getUserId())
                .orElse(null);
    }
    
    /**
     * 判断是否为默认密码（未设置密码）
     * 检查密码是否为BCrypt格式，如果不是则认为是默认密码
     */
    private boolean isDefaultPassword(String password) {
        if (password == null || password.isEmpty()) {
            return true;
        }
        // 如果是占位符，认为是默认密码
        if (password.equals(DEFAULT_PASSWORD_PLACEHOLDER)) {
            return true;
        }
        // 如果是占位符格式（$2a$05$placeholder），认为是默认密码
        if (password.contains("placeholder")) {
            return true;
        }
        // BCrypt格式以$2a$、$2b$或$2y$开头，且长度至少60字符
        // 如果不符合BCrypt格式，或长度不足，则认为是默认密码
        boolean isBcryptFormat = password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$");
        if (!isBcryptFormat) {
            return true; // 不是BCrypt格式，认为是默认密码
        }
        // 是BCrypt格式，但长度不足60字符，可能是占位符
        return password.length() < 60;
    }
}
