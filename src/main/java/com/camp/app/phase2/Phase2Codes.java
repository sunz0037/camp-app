package com.camp.app.phase2;

import com.camp.app.api.BizException;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 二期字符串枚举与库内 tinyint 的对照。
 */
public final class Phase2Codes {
    public static final String THEME_DEFAULT = "default";
    public static final Set<String> THEMES = Set.of(THEME_DEFAULT, "dark", "campus-green");
    public static final Set<String> EXAM_TYPES = Set.of("MIDTERM", "FINAL", "MONTHLY", "QUIZ");
    public static final String DEV_RESET_CODE = "000000";

    private static final Map<String, Integer> EXAM_STATUS = Map.of(
            "CANCELED", 0,
            "SCHEDULED", 1,
            "FINISHED", 2
    );
    private static final Map<Integer, String> EXAM_STATUS_NAME = Map.of(
            0, "CANCELED",
            1, "SCHEDULED",
            2, "FINISHED"
    );
    private static final Map<String, Integer> VISITOR_STATUS = Map.of(
            "PENDING", 0,
            "APPROVED", 1,
            "REJECTED", 2,
            "CHECKED_IN", 3,
            "CHECKED_OUT", 4
    );
    private static final Map<Integer, String> VISITOR_STATUS_NAME = Map.of(
            0, "PENDING",
            1, "APPROVED",
            2, "REJECTED",
            3, "CHECKED_IN",
            4, "CHECKED_OUT"
    );

    private Phase2Codes() {
    }

    public static String theme(String raw) {
        if (raw == null || raw.isBlank() || !THEMES.contains(raw.trim())) {
            throw BizException.badRequest("themeKey 只能是 default、dark、campus-green");
        }
        return raw.trim();
    }

    public static String examType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw BizException.badRequest("考试类型不能为空");
        }
        String value = raw.trim().toUpperCase(Locale.ROOT);
        if (!EXAM_TYPES.contains(value)) {
            throw BizException.badRequest("考试类型只能是 MIDTERM、FINAL、MONTHLY、QUIZ");
        }
        return value;
    }

    public static Integer examStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        Integer code = EXAM_STATUS.get(raw.trim().toUpperCase(Locale.ROOT));
        if (code == null) {
            throw BizException.badRequest("考试状态只能是 SCHEDULED、FINISHED、CANCELED");
        }
        return code;
    }

    public static String examStatusName(Integer code) {
        return code == null ? null : EXAM_STATUS_NAME.get(code);
    }

    public static Integer visitorStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        Integer code = VISITOR_STATUS.get(raw.trim().toUpperCase(Locale.ROOT));
        if (code == null) {
            throw BizException.badRequest("来访状态只能是 PENDING、APPROVED、REJECTED、CHECKED_IN、CHECKED_OUT");
        }
        return code;
    }

    public static String visitorStatusName(Integer code) {
        return code == null ? null : VISITOR_STATUS_NAME.get(code);
    }
}
