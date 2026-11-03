package com.camp.app.service;

public interface AuditLogService {
    void record(String action, Long actorId, String targetType, String targetId, String detail, boolean success);

    /**
     * 显式事件码。1–6 为首期学籍/登录事件，7–11 为二期写路径。
     * 明细禁止写入密码、身份证、手机号。
     */
    void recordEvent(int eventCode, Long actorId, Long targetUserId, String targetType, String targetId, String detail);
}
