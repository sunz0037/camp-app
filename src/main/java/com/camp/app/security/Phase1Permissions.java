package com.camp.app.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 与契约附录 A 一致的按钮权限码。库内无授权行时按角色给默认集。
 */
public final class Phase1Permissions {
    public static final String ORG_WRITE = "org:write";
    public static final String STUDENT_WRITE = "student:write";
    public static final String STUDENT_IMPORT = "student:import";
    public static final String STUDENT_EXPORT = "student:export";
    public static final String COURSE_WRITE = "course:write";
    public static final String CLASS_COURSE_WRITE = "class_course:write";
    public static final String ATTENDANCE_RECORD = "attendance:record";
    public static final String ANNOUNCEMENT_PUBLISH = "announcement:publish";
    public static final String ANNOUNCEMENT_TOP = "announcement:top";
    public static final String ANNOUNCEMENT_WITHDRAW = "announcement:withdraw";
    public static final String USER_WRITE = "user:write";
    public static final String ROLE_WRITE = "role:write";

    public static final List<String> ALL = List.of(
            ORG_WRITE, STUDENT_WRITE, STUDENT_IMPORT, STUDENT_EXPORT,
            COURSE_WRITE, CLASS_COURSE_WRITE, ATTENDANCE_RECORD,
            ANNOUNCEMENT_PUBLISH, ANNOUNCEMENT_TOP, ANNOUNCEMENT_WITHDRAW,
            USER_WRITE, ROLE_WRITE
    );

    private static final List<String> ACADEMIC = List.of(
            ORG_WRITE, STUDENT_WRITE, STUDENT_IMPORT, STUDENT_EXPORT,
            COURSE_WRITE, CLASS_COURSE_WRITE, ATTENDANCE_RECORD,
            ANNOUNCEMENT_PUBLISH, ANNOUNCEMENT_TOP, ANNOUNCEMENT_WITHDRAW,
            USER_WRITE
    );

    private static final List<String> TEACHER = List.of(
            STUDENT_EXPORT, ATTENDANCE_RECORD, ANNOUNCEMENT_PUBLISH
    );

    private Phase1Permissions() {
    }

    public static List<String> defaultsOf(String role) {
        return switch (role) {
            case Phase1Roles.ADMIN -> ALL;
            case Phase1Roles.ACADEMIC -> ACADEMIC;
            case Phase1Roles.TEACHER -> TEACHER;
            default -> List.of();
        };
    }

    public static List<String> defaults(Collection<String> roles) {
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        if (roles != null) {
            for (String role : roles) {
                codes.addAll(defaultsOf(role));
            }
        }
        return new ArrayList<>(codes);
    }

    public static boolean known(String code) {
        return ALL.contains(code);
    }

    public static boolean allows(Collection<String> permissions, String code) {
        return permissions != null && permissions.contains(code);
    }

    public static Set<String> allSet() {
        return Set.copyOf(ALL);
    }
}
