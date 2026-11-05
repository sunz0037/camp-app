package com.camp.app.attendance;

import java.util.Collection;

/**
 * 异常明细归入班级日汇总计数。汇总本身不带 courseId。
 */
public final class AttendanceCalculator {
    public record Counts(int absent, int late, int leave, int earlyLeave) {
    }

    private AttendanceCalculator() {
    }

    public static Counts fromTypes(Collection<String> exceptionTypes) {
        int absent = 0;
        int late = 0;
        int leave = 0;
        int earlyLeave = 0;
        if (exceptionTypes != null) {
            for (String type : exceptionTypes) {
                if (type == null) {
                    continue;
                }
                switch (type) {
                    case "ABSENT" -> absent++;
                    case "LATE" -> late++;
                    case "PERSONAL_LEAVE", "SICK_LEAVE" -> leave++;
                    case "EARLY_LEAVE" -> earlyLeave++;
                    default -> {
                    }
                }
            }
        }
        return new Counts(absent, late, leave, earlyLeave);
    }

    public static Integer legacyStatus(String exceptionType) {
        if (exceptionType == null) {
            return null;
        }
        return switch (exceptionType) {
            case "ABSENT" -> 1;
            case "LATE" -> 2;
            case "PERSONAL_LEAVE", "SICK_LEAVE" -> 3;
            case "EARLY_LEAVE" -> 4;
            default -> null;
        };
    }

    public static int codeOf(String exceptionType) {
        if (exceptionType == null) {
            throw new IllegalArgumentException("exceptionType 不能为空");
        }
        return switch (exceptionType) {
            case "LATE" -> 1;
            case "EARLY_LEAVE" -> 2;
            case "PERSONAL_LEAVE" -> 3;
            case "SICK_LEAVE" -> 4;
            case "ABSENT" -> 5;
            default -> throw new IllegalArgumentException("未知异常类型: " + exceptionType);
        };
    }

    public static String nameOf(Integer code) {
        if (code == null) {
            return null;
        }
        return switch (code) {
            case 1 -> "LATE";
            case 2 -> "EARLY_LEAVE";
            case 3 -> "PERSONAL_LEAVE";
            case 4 -> "SICK_LEAVE";
            case 5 -> "ABSENT";
            default -> null;
        };
    }

    public static int confirmCode(String status) {
        if (status == null || status.isBlank()) {
            return 2;
        }
        return switch (status) {
            case "PENDING" -> 1;
            case "CLEARED" -> 3;
            default -> 2;
        };
    }

    public static String confirmName(Integer code) {
        if (code == null) {
            return "CONFIRMED";
        }
        return switch (code) {
            case 1 -> "PENDING";
            case 3 -> "CLEARED";
            default -> "CONFIRMED";
        };
    }

    public static Integer legacyStatusToCode(Integer legacyStatus) {
        if (legacyStatus == null) {
            return null;
        }
        return switch (legacyStatus) {
            case 1 -> 5;
            case 2 -> 1;
            case 3 -> 3;
            case 4 -> 2;
            default -> legacyStatus;
        };
    }

    public static String exceptionTypeOf(Integer status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case 1 -> "ABSENT";
            case 2 -> "LATE";
            case 3 -> "PERSONAL_LEAVE";
            case 4 -> "EARLY_LEAVE";
            default -> null;
        };
    }
}
