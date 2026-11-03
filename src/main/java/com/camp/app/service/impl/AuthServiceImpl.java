package com.camp.app.service.impl;

import com.camp.app.api.BizException;
import com.camp.app.entity.Student;
import com.camp.app.entity.User;
import com.camp.app.model.ActivateUserRequest;
import com.camp.app.model.AdminResetPasswordRequest;
import com.camp.app.model.ChangePasswordRequest;
import com.camp.app.model.DataScope;
import com.camp.app.model.LoginRequest;
import com.camp.app.model.LoginResponse;
import com.camp.app.model.ProfileUpdateRequest;
import com.camp.app.repository.StudentRepository;
import com.camp.app.repository.UserRepository;
import com.camp.app.security.AuthContext;
import com.camp.app.security.AuthPrincipal;
import com.camp.app.security.JwtService;
import com.camp.app.security.LoginGuard;
import com.camp.app.security.PasswordHistory;
import com.camp.app.security.PasswordPolicy;
import com.camp.app.security.RefreshTokenStore;
import com.camp.app.security.SessionControl;
import com.camp.app.security.Phase1Roles;
import com.camp.app.service.AccessService;
import com.camp.app.service.AuditLogService;
import com.camp.app.service.AuthService;
import com.camp.app.util.PasswordEncoderUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final JwtService jwtService;
    private final RefreshTokenStore refreshTokenStore;
    private final AccessService accessService;
    private final AuditLogService auditLogService;
    private final PasswordPolicy passwordPolicy;
    private final PasswordHistory passwordHistory;
    private final LoginGuard loginGuard;
    private final SessionControl sessionControl;

    private static final String DEFAULT_PASSWORD_PLACEHOLDER = "DEFAULT_PASSWORD_NOT_SET";
    private static final String FORCE_PREFIX = "FORCE$";

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        if (request.getIdentityType() != null && request.getIdentityType() == 3) {
            throw BizException.badRequest("家长与学生同权，请使用 STUDENT 账号登录");
        }
        String account = firstText(request.getUsername(), request.getIdentifier());
        if (!StringUtils.hasText(account)) {
            throw BizException.badRequest("用户名不能为空");
        }
        User user = resolveUser(request, account);
        if (user == null) {
            auditLogService.record("LOGIN_FAIL", null, "USER", null, "账号不存在", false);
            throw BizException.unauthorized("账号或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() == 0) {
            auditLoginFail(user, "用户已禁用");
            throw BizException.unauthorized("用户已被禁用，请联系管理员");
        }
        rejectIfLocked(user);
        if (isUnsetPassword(user.getPassword())) {
            boolean nameOk = StringUtils.hasText(request.getRealName())
                    && user.getRealName().equals(request.getRealName().trim());
            boolean passwordOk = safeMatch(request.getPassword(), user.getPassword());
            if (!nameOk && !passwordOk) {
                auditLoginFail(user, "首次登录验证失败");
                failLogin(user);
                throw BizException.unauthorized("首次登录需要提供匹配的姓名，或由管理员重置密码后再登录");
            }
            loginGuard.unlock(user.getId());
            return issue(user, true, "请先设置密码");
        }
        if (isForcedChange(user.getPassword())) {
            if (!safeMatch(request.getPassword(), user.getPassword())) {
                auditLoginFail(user, "临时密码错误");
                failLogin(user);
                throw BizException.unauthorized("账号或密码错误");
            }
            loginGuard.unlock(user.getId());
            return issue(user, true, "请先修改临时密码");
        }
        if (!safeMatch(request.getPassword(), user.getPassword())) {
            auditLoginFail(user, "密码错误");
            failLogin(user);
            throw BizException.unauthorized("账号或密码错误");
        }
        loginGuard.unlock(user.getId());
        return issue(user, false, null);
    }

    @Override
    @Transactional
    public LoginResponse activateUser(ActivateUserRequest request) {
        Long currentUserId = AuthContext.userId();
        if (currentUserId != null) {
            User user = userRepository.findById(currentUserId)
                    .orElseThrow(() -> BizException.notFound("用户不存在"));
            String newPassword = firstText(request.getNewPassword(), request.getPassword());
            if (StringUtils.hasText(request.getPhone())) {
                user.setPhone(request.getPhone().trim());
            }
            assertPasswordAcceptable(user, newPassword);
            storePassword(user, newPassword, "首次设置密码");
            return issue(user, false, "已完成首次改密");
        }
        if (request.getIdentityType() != null && request.getIdentityType() != 1 && request.getIdentityType() != 2) {
            throw BizException.badRequest("身份类型错误，仅支持学生和教工");
        }
        if (!StringUtils.hasText(request.getIdentifier()) || !StringUtils.hasText(request.getRealName())) {
            throw BizException.unauthorized("请先登录后再修改初始密码");
        }
        User user = request.getIdentityType() != null && request.getIdentityType() == 2
                ? userRepository.findByUsername(request.getIdentifier()).orElse(null)
                : findStudentByStudentNo(request.getIdentifier());
        if (request.getIdentityType() == null) {
            user = userRepository.findByUsername(request.getIdentifier()).orElse(null);
            if (user == null) {
                user = findStudentByStudentNo(request.getIdentifier());
            }
        }
        if (user == null) {
            throw BizException.notFound("账号不存在");
        }
        if (!user.getRealName().equals(request.getRealName().trim())) {
            throw BizException.badRequest("姓名不匹配");
        }
        String newPassword = firstText(request.getNewPassword(), request.getPassword());
        if (StringUtils.hasText(request.getPhone())) {
            user.setPhone(request.getPhone().trim());
        }
        assertPasswordAcceptable(user, newPassword);
        storePassword(user, newPassword, "首次设置密码");
        return issue(user, false, "激活成功，已设置密码");
    }

    @Override
    @Transactional
    public LoginResponse refresh(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw BizException.unauthorized("缺少 refreshToken");
        }
        Claims claims;
        try {
            claims = jwtService.parse(refreshToken);
        } catch (Exception ex) {
            throw BizException.unauthorized("刷新令牌无效");
        }
        if (!"refresh".equals(claims.get("typ", String.class))) {
            throw BizException.unauthorized("刷新令牌无效");
        }
        RefreshTokenStore.Token stored = refreshTokenStore.find(claims.getId())
                .orElseThrow(() -> BizException.unauthorized("刷新令牌无效"));
        if (stored.revoked() || stored.expiresAt().isBefore(LocalDateTime.now())) {
            throw BizException.unauthorized("刷新令牌已失效");
        }
        refreshTokenStore.revoke(claims.getId());
        User user = userRepository.findById(stored.userId())
                .orElseThrow(() -> BizException.unauthorized("用户不存在"));
        return issue(user, mustChangePassword(user), null);
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            return;
        }
        try {
            Claims claims = jwtService.parse(refreshToken);
            refreshTokenStore.revoke(claims.getId());
        } catch (Exception ex) {
            log.info("登出时刷新令牌无法解析，按已登出处理");
        }
    }

    @Override
    @Transactional
    public LoginResponse changePassword(Long userId, String oldPassword, String newPassword) {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword(oldPassword);
        request.setNewPassword(newPassword);
        return changePassword(userId, request);
    }

    @Override
    @Transactional
    public LoginResponse changePassword(Long userId, ChangePasswordRequest request) {
        if (userId == null) {
            throw BizException.unauthorized("未登录");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BizException.notFound("用户不存在"));
        if (request == null || !safeMatch(request.getOldPassword(), user.getPassword())) {
            auditPassword(user, "旧密码错误", false);
            throw BizException.badRequest("旧密码错误");
        }
        assertPasswordAcceptable(user, request.getNewPassword());
        storePassword(user, request.getNewPassword(), "用户修改密码");
        return issue(user, false, "密码已更新");
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse.UserInfo getCurrentUser(Long userId) {
        if (userId == null) {
            throw BizException.unauthorized("未登录");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BizException.notFound("用户不存在"));
        return userInfo(user);
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BizException.notFound("用户不存在"));
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        userRepository.save(user);
    }

    @Override
    @Transactional
    public String adminResetPassword(Long operatorId, AdminResetPasswordRequest request) {
        if (request == null || request.getUserId() == null) {
            throw BizException.badRequest("userId 不能为空");
        }
        User target = userRepository.findById(request.getUserId())
                .orElseThrow(() -> BizException.notFound("用户不存在"));
        assertCanManage(target);
        String temp = StringUtils.hasText(request.getTemporaryPassword())
                ? request.getTemporaryPassword().trim()
                : passwordPolicy.generate();
        assertPasswordAcceptable(target, temp);
        String previous = target.getPassword();
        target.setPassword(FORCE_PREFIX + PasswordEncoderUtil.encode(temp));
        userRepository.save(target);
        rememberPrevious(target.getId(), previous);
        invalidateSessions(target.getId());
        Long actor = operatorId != null ? operatorId : target.getId();
        auditLogService.record("PASSWORD_RESET", actor, "USER", String.valueOf(target.getId()), "管理员重置为临时口令并强制改密", true);
        return temp;
    }

    @Override
    @Transactional(readOnly = true)
    public void adminUnlock(Long operatorId, Long userId) {
        if (userId == null) {
            throw BizException.badRequest("userId 不能为空");
        }
        User target = userRepository.findById(userId)
                .orElseThrow(() -> BizException.notFound("用户不存在"));
        assertCanManage(target);
        loginGuard.unlock(target.getId());
        log.info("已解除登录锁定 operator={} target={}", operatorId, userId);
    }

    private LoginResponse issue(User user, boolean firstLogin, String message) {
        List<String> roles = accessService.rolesOf(user);
        List<String> permissions = accessService.permissionsOf(user);
        String access = jwtService.createAccessToken(
                user.getId(), roles, permissions, firstLogin, sessionControl.current(user.getId()));
        String jti = jwtService.newJti();
        String refresh = jwtService.createRefreshToken(user.getId(), jti);
        refreshTokenStore.save(jti, user.getId(), LocalDateTime.now().plusSeconds(jwtService.getRefreshTtlSeconds()));
        return LoginResponse.builder()
                .isFirstLogin(firstLogin)
                .accessToken(access)
                .token(access)
                .refreshToken(refresh)
                .userInfo(userInfo(user))
                .message(message)
                .build();
    }

    private LoginResponse.UserInfo userInfo(User user) {
        DataScope scope = accessService.scopeOf(user.getId());
        return LoginResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .userType(user.getUserType())
                .avatarUrl(user.getAvatarUrl())
                .roles(accessService.rolesOf(user))
                .permissions(accessService.permissionsOf(user))
                .dataScope(scope)
                .build();
    }

    private User resolveUser(LoginRequest request, String account) {
        if (request.getIdentityType() != null && request.getIdentityType() == 1) {
            return findStudentByStudentNo(account);
        }
        if (request.getIdentityType() != null && request.getIdentityType() == 2) {
            Optional<User> staff = userRepository.findByUsernameAndUserType(account, 2);
            if (staff.isPresent()) {
                return staff.get();
            }
            return userRepository.findByUsername(account).filter(user -> user.getUserType() != null && user.getUserType() != 1).orElse(null);
        }
        Optional<User> byName = userRepository.findByUsername(account);
        if (byName.isPresent()) {
            return byName.get();
        }
        return findStudentByStudentNo(account);
    }

    private User findStudentByStudentNo(String studentNo) {
        Optional<Student> studentOpt = studentRepository.findByStudentNo(studentNo);
        if (studentOpt.isEmpty() || studentOpt.get().getUserId() == null) {
            return null;
        }
        return userRepository.findById(studentOpt.get().getUserId()).orElse(null);
    }

    private boolean mustChangePassword(User user) {
        return isUnsetPassword(user.getPassword()) || isForcedChange(user.getPassword());
    }

    private boolean isForcedChange(String password) {
        return password != null && password.startsWith(FORCE_PREFIX);
    }

    private boolean isUnsetPassword(String password) {
        if (isForcedChange(password)) {
            return false;
        }
        if (password == null || password.isEmpty() || password.equals(DEFAULT_PASSWORD_PLACEHOLDER) || password.contains("placeholder")) {
            return true;
        }
        boolean bcrypt = password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$");
        return !bcrypt || password.length() < 60;
    }

    private boolean safeMatch(String raw, String encoded) {
        try {
            return PasswordEncoderUtil.matches(raw, encodedForMatch(encoded));
        } catch (Exception ex) {
            return false;
        }
    }

    private String encodedForMatch(String encoded) {
        if (isForcedChange(encoded)) {
            return encoded.substring(FORCE_PREFIX.length());
        }
        return encoded;
    }

    private void rejectIfLocked(User user) {
        try {
            loginGuard.ensureAllowed(user.getId());
        } catch (BizException ex) {
            auditLoginFail(user, "账号锁定");
            throw ex;
        }
    }

    private void failLogin(User user) {
        loginGuard.onFailure(user.getId());
    }

    private void auditLoginFail(User user, String detail) {
        String target = user.getId() == null ? null : String.valueOf(user.getId());
        auditLogService.record("LOGIN_FAIL", user.getId(), "USER", target, detail, false);
    }

    private void auditPassword(User user, String detail, boolean success) {
        auditLogService.record("PASSWORD_RESET", user.getId(), "USER", String.valueOf(user.getId()), detail, success);
    }

    private void assertPasswordAcceptable(User user, String raw) {
        try {
            passwordPolicy.check(raw, user.getUsername(), encodedForMatch(user.getPassword()), passwordHistory.olderHashes(user.getId()));
        } catch (BizException ex) {
            auditPassword(user, ex.getMessage(), false);
            throw ex;
        }
    }

    private void storePassword(User user, String raw, String auditDetail) {
        String previous = user.getPassword();
        user.setPassword(PasswordEncoderUtil.encode(raw));
        userRepository.save(user);
        rememberPrevious(user.getId(), previous);
        invalidateSessions(user.getId());
        auditPassword(user, auditDetail, true);
    }

    private void rememberPrevious(Long userId, String previous) {
        if (previous == null || isUnsetPassword(previous)) {
            return;
        }
        passwordHistory.remember(userId, encodedForMatch(previous));
    }

    private void invalidateSessions(Long userId) {
        sessionControl.bump(userId);
        refreshTokenStore.revokeUser(userId);
        loginGuard.unlock(userId);
    }

    private void assertCanManage(User target) {
        AuthPrincipal principal = AuthContext.get();
        if (principal == null || (!principal.roles().contains(Phase1Roles.ADMIN) && !principal.roles().contains(Phase1Roles.ACADEMIC))) {
            throw BizException.forbidden("仅管理员或教务可重置密码");
        }
        if (!principal.roles().contains(Phase1Roles.ADMIN)) {
            List<String> targetRoles = accessService.rolesOf(target);
            String highest = Phase1Roles.highest(targetRoles);
            if (Phase1Roles.ADMIN.equals(highest) || Phase1Roles.ACADEMIC.equals(highest)) {
                throw BizException.forbidden("教务不能重置管理员或教务账号");
            }
        }
    }

    private String firstText(String a, String b) {
        if (StringUtils.hasText(a)) {
            return a.trim();
        }
        if (StringUtils.hasText(b)) {
            return b.trim();
        }
        return null;
    }

}
