package com.camp.app.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_logs")
@Data
public class PasswordResetLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "identifier", length = 50)
    private String identifier;
    
    @Column(name = "identity_type")
    private Integer identityType; // 1-学生，2-教工
    
    @Column(name = "reset_type", nullable = false)
    private Integer resetType = 1; // 1-学生六要素，2-教工手机验证码
    
    @Column(name = "status", nullable = false)
    private Integer status; // 0-失败，1-成功
    
    @Column(name = "failure_reason", length = 255)
    private String failureReason;
    
    @Column(name = "ip_address", length = 50)
    private String ipAddress;
    
    @Column(name = "user_agent", length = 255)
    private String userAgent;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

