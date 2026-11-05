package com.camp.app.model;

import lombok.Data;

import java.util.List;

@Data
public class PermissionGrantRequest {
    private List<String> permissionCodes;
}
