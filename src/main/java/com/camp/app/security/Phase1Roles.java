package com.camp.app.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 首期四档角色。旧码 GRADE_ADMIN 读入时折叠为 ACADEMIC；LOGISTICS / VISITOR 不进入首期角色集。
 */
public final class Phase1Roles {
    public static final String ADMIN = "ADMIN";
    public static final String ACADEMIC = "ACADEMIC";
    public static final String TEACHER = "TEACHER";
    public static final String STUDENT = "STUDENT";

    private static final List<String> ORDER = List.of(ADMIN, ACADEMIC, TEACHER, STUDENT);

    private Phase1Roles() {
    }

    public static List<String> normalize(Collection<String> raw) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (raw == null) {
            return new ArrayList<>();
        }
        for (String role : raw) {
            if (role == null) {
                continue;
            }
            switch (role) {
                case "GRADE_ADMIN" -> out.add(ACADEMIC);
                case "LOGISTICS", "VISITOR" -> {
                }
                case ADMIN, ACADEMIC, TEACHER, STUDENT -> out.add(role);
                default -> {
                }
            }
        }
        return new ArrayList<>(out);
    }

    public static String highest(Collection<String> roles) {
        for (String role : ORDER) {
            if (roles != null && roles.contains(role)) {
                return role;
            }
        }
        return STUDENT;
    }

    public static String fromUserType(Integer userType) {
        if (userType == null) {
            return STUDENT;
        }
        return switch (userType) {
            case 3 -> ADMIN;
            case 2 -> TEACHER;
            default -> STUDENT;
        };
    }

    public static boolean isPhase1Code(String code) {
        return ADMIN.equals(code) || ACADEMIC.equals(code) || TEACHER.equals(code) || STUDENT.equals(code);
    }
}
