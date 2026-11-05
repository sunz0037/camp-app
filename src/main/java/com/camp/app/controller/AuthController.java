package com.camp.app.controller;

import com.camp.app.api.BaseResponse;
import com.camp.app.model.ActivateUserRequest;
import com.camp.app.model.AdminResetPasswordRequest;
import com.camp.app.model.AdminUnlockRequest;
import com.camp.app.model.ChangePasswordRequest;
import com.camp.app.model.LoginRequest;
import com.camp.app.model.LoginResponse;
import com.camp.app.model.ProfileUpdateRequest;
import com.camp.app.model.RefreshTokenRequest;
import com.camp.app.security.AuthContext;
import com.camp.app.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public BaseResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        return BaseResponse.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public BaseResponse<LoginResponse> refresh(@RequestBody(required = false) RefreshTokenRequest body,
                                               @RequestParam(required = false) String refreshToken) {
        String token = body != null && body.getRefreshToken() != null ? body.getRefreshToken() : refreshToken;
        return BaseResponse.ok(authService.refresh(token));
    }

    @PostMapping("/logout")
    public BaseResponse<Void> logout(@RequestBody(required = false) RefreshTokenRequest body,
                                     @RequestParam(required = false) String refreshToken) {
        String token = body != null && body.getRefreshToken() != null ? body.getRefreshToken() : refreshToken;
        authService.logout(token);
        return BaseResponse.ok(null);
    }

    @GetMapping("/me")
    public BaseResponse<LoginResponse.UserInfo> me(@RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {
        Long userId = AuthContext.userId() != null ? AuthContext.userId() : headerUserId;
        return BaseResponse.ok(authService.getCurrentUser(userId));
    }

    @PutMapping("/profile")
    public BaseResponse<Void> profile(@RequestBody ProfileUpdateRequest request) {
        authService.updateProfile(AuthContext.userId(), request);
        return BaseResponse.ok(null);
    }

    @PutMapping("/password")
    public BaseResponse<LoginResponse> changePassword(@RequestBody(required = false) ChangePasswordRequest body,
                                                      @RequestParam(required = false) Long userId,
                                                      @RequestParam(required = false) String oldPassword,
                                                      @RequestParam(required = false) String newPassword) {
        Long id = AuthContext.userId() != null ? AuthContext.userId() : userId;
        LoginResponse session = body != null && body.getNewPassword() != null
                ? authService.changePassword(id, body)
                : authService.changePassword(id, oldPassword, newPassword);
        return BaseResponse.ok(session);
    }

    @PostMapping("/change-password")
    public BaseResponse<LoginResponse> changePasswordPost(@RequestBody ChangePasswordRequest body) {
        return BaseResponse.ok(authService.changePassword(AuthContext.userId(), body));
    }

    @PostMapping("/activate")
    public BaseResponse<LoginResponse> activate(@RequestBody ActivateUserRequest request) {
        return BaseResponse.ok(authService.activateUser(request));
    }

    @PostMapping("/admin-reset-password")
    public BaseResponse<Map<String, String>> adminReset(@RequestBody AdminResetPasswordRequest request) {
        String temporary = authService.adminResetPassword(AuthContext.userId(), request);
        return BaseResponse.ok(Map.of("temporaryPassword", temporary, "mustChangePassword", "true"));
    }

    @PostMapping("/admin-unlock")
    public BaseResponse<Void> adminUnlock(@RequestBody AdminUnlockRequest request) {
        Long userId = request == null ? null : request.getUserId();
        authService.adminUnlock(AuthContext.userId(), userId);
        return BaseResponse.ok(null);
    }
}
