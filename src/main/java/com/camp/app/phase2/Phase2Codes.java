package com.camp.app.phase2;

import com.camp.app.api.BizException;

import java.util.Locale;
import java.util.Set;

public final class Phase2Codes {
    public static final String THEME_DEFAULT = "default";
    public static final Set<String> THEMES = Set.of(THEME_DEFAULT, "dark", "campus-green", "campus-blue");
    public static final Set<String> VISITOR_STATUSES = Set.of(
            "PENDING", "APPROVED", "REJECTED", "CHECKED_IN", "CHECKED_OUT", "CANCELLED");

    private Phase2Codes() {
    }

    public static String theme(String raw) {
        if (raw == null || raw.isBlank() || !THEMES.contains(raw.trim())) {
            throw BizException.badRequest("themeKey 只能是 default、dark、campus-green、campus-blue");
        }
        return raw.trim();
    }

    public static String visitorStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim().toUpperCase(Locale.ROOT);
        if (!VISITOR_STATUSES.contains(value)) {
            throw BizException.badRequest("来访状态只能是 PENDING、APPROVED、REJECTED、CHECKED_IN、CHECKED_OUT、CANCELLED");
        }
        return value;
    }
}
