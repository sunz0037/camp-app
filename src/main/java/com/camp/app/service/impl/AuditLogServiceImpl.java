package com.camp.app.service.impl;

import com.camp.app.entity.AuditLog;
import com.camp.app.repository.AuditLogRepository;
import com.camp.app.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {
    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, Long actorId, String targetType, String targetId, String detail, boolean success) {
        write(eventCode(action), actorId, null, targetType, targetId, action, detail, success);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordEvent(int eventCode, Long actorId, Long targetUserId, String targetType, String targetId, String detail) {
        write(eventCode, actorId, targetUserId, targetType, targetId, eventType(eventCode), detail, true);
    }

    private void write(int code, Long actorId, Long targetUserId, String targetType, String targetId,
                       String summary, String detail, boolean success) {
        try {
            AuditLog row = new AuditLog();
            row.setEventType(eventType(code));
            row.setEventCode(code);
            row.setActorUserId(actorId);
            row.setTargetUserId(targetUserId);
            row.setTargetType(targetType);
            Long parsedTarget = parseId(targetId);
            row.setTargetId(parsedTarget);
            if ("USER".equals(targetType)) {
                row.setTargetUserId(parsedTarget != null ? parsedTarget : actorId);
            }
            row.setResult(success ? 1 : 0);
            row.setSummary(summary == null ? eventType(code) : summary.substring(0, Math.min(summary.length(), 255)));
            row.setDetail(detail == null ? null : detail.substring(0, Math.min(detail.length(), 1000)));
            auditLogRepository.save(row);
        } catch (Exception ex) {
            log.warn("审计写入失败: {}", ex.getMessage());
        }
    }

    private String eventType(int code) {
        return switch (code) {
            case 1 -> "LOGIN_FAIL";
            case 2 -> "PASSWORD_RESET";
            case 3 -> "STUDENT_IMPORT";
            case 4 -> "STUDENT_EXPORT";
            case 5 -> "STUDENT_STATUS_CHANGE";
            case 6 -> "STUDENT_PROFILE_CHANGE";
            case 7 -> "USER_ACCOUNT";
            case 8 -> "COURSE_WRITE";
            case 9 -> "CLASS_COURSE_WRITE";
            case 10 -> "ATTENDANCE_WRITE";
            case 11 -> "ANNOUNCEMENT_WRITE";
            default -> "STUDENT_PROFILE_CHANGE";
        };
    }

    private String eventType(String action) {
        return eventType(eventCode(action));
    }

    private int eventCode(String action) {
        if (action == null) {
            return 6;
        }
        if (action.contains("LOGIN")) {
            return 1;
        }
        if (action.contains("PASSWORD") || action.contains("RESET")) {
            return 2;
        }
        if (action.contains("IMPORT")) {
            return 3;
        }
        if (action.contains("EXPORT")) {
            return 4;
        }
        if (action.contains("STATUS")) {
            return 5;
        }
        return 6;
    }

    private Long parseId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
