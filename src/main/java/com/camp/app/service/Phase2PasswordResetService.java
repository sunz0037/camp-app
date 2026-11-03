package com.camp.app.service;

import com.camp.app.model.Phase2ResetPasswordRequest;

public interface Phase2PasswordResetService {
    void reset(Phase2ResetPasswordRequest request);
}
