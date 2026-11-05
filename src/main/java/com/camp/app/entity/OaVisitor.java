package com.camp.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "oa_visitors")
public class OaVisitor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(name = "id_no", length = 18)
    private String idNo;

    @Column(length = 100)
    private String org;

    @Column(nullable = false, length = 200)
    private String purpose;

    @Column(name = "host_user_id", nullable = false)
    private Long hostUserId;

    @Column(name = "host_dept", length = 100)
    private String hostDept;

    @Column(name = "expected_arrive", nullable = false)
    private LocalDateTime expectedArrive;

    @Column(name = "expected_leave")
    private LocalDateTime expectedLeave;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null || status.isBlank()) {
            status = "PENDING";
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
