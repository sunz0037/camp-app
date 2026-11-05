package com.camp.app.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "oa_announcements")
@Data
public class Announcement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 50)
    private String category;

    @Column(name = "publisher_id", nullable = false)
    private Long publisherId;

    @Column(nullable = false)
    private LocalDateTime publishTime;

    private LocalDateTime expireTime;

    @Column(nullable = false)
    private Integer isTop = 0;

    @Column(nullable = false)
    private Integer isUrgent = 0;

    @Column(length = 50)
    private String targetAudience;

    @Column(nullable = false)
    private Integer viewCount = 0;

    @Column(length = 500)
    private String attachmentUrl;

    @Column(nullable = false)
    private Integer status = 1; // 0-草稿，1-已发布，2-已撤回

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (publishTime == null) {
            publishTime = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

