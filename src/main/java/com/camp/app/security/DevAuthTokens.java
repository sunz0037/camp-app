package com.camp.app.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 开发态身份解析。不访问数据库。
 * Authorization: Bearer dev-ADMIN|dev-ACADEMIC|dev-TEACHER|dev-STUDENT
 * 或 X-Dev-Role 同名角色。
 */
public final class DevAuthTokens {
    public static final Set<String> ROLES = Set.of(
            Phase1Roles.ADMIN, Phase1Roles.ACADEMIC, Phase1Roles.TEACHER, Phase1Roles.STUDENT);

    private DevAuthTokens() {
    }

    public static boolean attempted(String authorization, String devRoleHeader) {
        if (devRoleHeader != null && !devRoleHeader.isBlank()) {
            return true;
        }
        return authorization != null && authorization.regionMatches(true, 0, "Bearer dev-", 0, "Bearer dev-".length());
    }

    public static AuthPrincipal resolve(String authorization, String devRoleHeader, String userIdHeader,
                                        String classIdsHeader, String gradeIdsHeader) {
        String role = normalizeRole(devRoleHeader);
        if (role == null) {
            role = roleFromAuthorization(authorization);
        }
        if (role == null || !ROLES.contains(role)) {
            throw new IllegalArgumentException("开发态角色必须是 ADMIN、ACADEMIC、TEACHER、STUDENT");
        }
        Long userId = parseLong(userIdHeader);
        if (userId == null) {
            userId = defaultUserId(role);
        }
        List<String> roles = List.of(role);
        return new AuthPrincipal(
                userId,
                roles,
                Phase1Permissions.defaults(roles),
                false,
                true,
                parseIdList(gradeIdsHeader),
                parseIdList(classIdsHeader));
    }

    static String normalizeRole(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        if (value.regionMatches(true, 0, "dev-", 0, 4)) {
            value = value.substring(4);
        }
        return value.toUpperCase(Locale.ROOT);
    }

    private static String roleFromAuthorization(String authorization) {
        if (authorization == null) {
            return null;
        }
        String value = authorization.trim();
        if (!value.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        return normalizeRole(value.substring(7).trim());
    }

    private static Long defaultUserId(String role) {
        return switch (role) {
            case Phase1Roles.ADMIN -> 1L;
            case Phase1Roles.ACADEMIC -> 2L;
            case Phase1Roles.TEACHER -> 3L;
            default -> 4L;
        };
    }

    private static Long parseLong(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Long.valueOf(raw.trim());
    }

    /**
     * @return null 表示请求头未出现；空列表表示显式传了空范围
     */
    static List<Long> parseIdList(String raw) {
        if (raw == null) {
            return null;
        }
        if (raw.isBlank()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (String part : raw.split(",")) {
            if (!part.isBlank()) {
                ids.add(Long.valueOf(part.trim()));
            }
        }
        return ids;
    }
}
