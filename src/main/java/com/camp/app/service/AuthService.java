package com.camp.app.service;

import com.camp.app.model.ActivateUserRequest;
import com.camp.app.model.AdminResetPasswordRequest;
import com.camp.app.model.ChangePasswordRequest;
import com.camp.app.model.LoginRequest;
import com.camp.app.model.LoginResponse;
import com.camp.app.model.ProfileUpdateRequest;

public interface AuthService {
    LoginResponse login(LoginRequest request);

    LoginResponse activateUser(ActivateUserRequest request);

    LoginResponse refresh(String refreshToken);

    void logout(String refreshToken);

    void changePassword(Long userId, String oldPassword, String newPassword);

    void changePassword(Long userId, ChangePasswordRequest request);

    LoginResponse.UserInfo getCurrentUser(Long userId);

    void updateProfile(Long userId, ProfileUpdateRequest request);

    String adminResetPassword(Long operatorId, AdminResetPasswordRequest request);
}
