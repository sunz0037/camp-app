package com.camp.app.service;

public interface AuditLogService {
    void record(String action, Long actorId, String targetType, String targetId, String detail, boolean success);
}
