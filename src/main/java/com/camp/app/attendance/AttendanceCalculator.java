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
