package com.camp.app.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Refresh Token 吊销表。DDL 以数据库任务为准（预期 db/phase1/）。
 */
@Data
@Entity
@Table(name = "auth_refresh_tokens", indexes = @Index(name = "idx_refresh_user", columnList = "user_id"))
public class AuthRefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String jti;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private Boolean revoked = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (revoked == null) {
            revoked = false;
        }
    }
}
