package com.camp.app.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告受众。scope_type：1 ALL，2 GRADE，3 CLASS。ALL 的 scope_id 固定为 0。
 */
@Data
@Entity
@Table(name = "oa_announcement_scopes", uniqueConstraints = @UniqueConstraint(name = "uk_announcement_scope", columnNames = {"announcement_id", "scope_type", "scope_id"}))
public class AnnouncementScope {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "announcement_id", nullable = false)
    private Long announcementId;

    @Column(name = "scope_type", nullable = false)
    private Integer scopeType;

    @Column(name = "scope_id", nullable = false)
    private Long scopeId = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (scopeId == null) {
            scopeId = 0L;
        }
    }
}
