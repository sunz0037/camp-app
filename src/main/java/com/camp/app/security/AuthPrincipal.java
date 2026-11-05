package com.camp.app.security;

import java.util.List;

import java.util.List;

/**
 * devGradeIds / devClassIds 为 null 表示请求没带对应头，数据范围回退到关联表。
 */
public record AuthPrincipal(
        Long userId,
        List<String> roles,
        List<String> permissions,
        boolean mustChangePassword,
        boolean devAuth,
        List<Long> devGradeIds,
        List<Long> devClassIds) {

    public AuthPrincipal(Long userId, List<String> roles, List<String> permissions, boolean mustChangePassword) {
        this(userId, roles, permissions, mustChangePassword, false, null, null);
    }

    public boolean hasPermission(String code) {
        return Phase1Roles.ADMIN.equals(Phase1Roles.highest(roles)) || Phase1Permissions.allows(permissions, code);
    }
}
