package com.camp.app.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告已读。(announcement_id, user_id) 唯一。
 * DDL 以数据库任务为准（预期 db/phase1/）。
 */
@Data
@Entity
@Table(name = "oa_announcement_reads", uniqueConstraints = @UniqueConstraint(name = "uk_announcement_user", columnNames = {"announcement_id", "user_id"}))
public class AnnouncementRead {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "announcement_id", nullable = false)
    private Long announcementId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "read_at", nullable = false)
    private LocalDateTime readAt;

    @PrePersist
    void onCreate() {
        if (readAt == null) {
            readAt = LocalDateTime.now();
        }
    }
}
