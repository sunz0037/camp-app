package com.camp.app.model;

import lombok.Data;

@Data
public class ProfileUpdateRequest {
    private String email;
    private String avatarUrl;
    private String phone;
}
