package com.camp.app.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录失败、密码重置、学生导入导出、学籍关键变更。
 * DDL 以数据库任务为准（预期 db/phase1/）。
 */
@Data
@Entity
@Table(name = "audit_logs", indexes = @Index(name = "idx_audit_action", columnList = "action"))
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "target_type", length = 50)
    private String targetType;

    @Column(name = "target_id", length = 64)
    private String targetId;

    @Column(length = 500)
    private String detail;

    @Column(nullable = false)
    private Boolean success = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (success == null) {
            success = true;
        }
    }
}
