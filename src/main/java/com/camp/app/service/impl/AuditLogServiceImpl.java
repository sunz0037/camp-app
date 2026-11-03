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
        try {
            AuditLog row = new AuditLog();
            row.setAction(action);
            row.setActorId(actorId);
            row.setTargetType(targetType);
            row.setTargetId(targetId);
            row.setDetail(detail == null ? null : detail.substring(0, Math.min(detail.length(), 500)));
            row.setSuccess(success);
            auditLogRepository.save(row);
        } catch (Exception ex) {
            log.warn("审计写入失败: {}", ex.getMessage());
        }
    }
}
