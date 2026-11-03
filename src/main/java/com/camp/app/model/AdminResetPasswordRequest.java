package com.camp.app.model;

import lombok.Data;

@Data
public class AdminResetPasswordRequest {
    private Long userId;
    private String temporaryPassword;
}
