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

    @Column(name = "visit_no", nullable = false, unique = true, length = 40)
    private String visitNo;

    @Column(name = "visitor_name", nullable = false, length = 50)
    private String visitorName;

    @Column(name = "id_card", length = 32)
    private String idCard;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 200)
    private String purpose;

    @Column(name = "host_user_id")
    private Long hostUserId;

    @Column(name = "host_name", length = 50)
    private String hostName;

    @Column(name = "appointment_time")
    private LocalDateTime appointmentTime;

    @Column(name = "arrival_time")
    private LocalDateTime arrivalTime;

    @Column(name = "leave_time")
    private LocalDateTime leaveTime;

    @Column(nullable = false)
    private Integer status;

    @Column(length = 500)
    private String remark;

    @Column(name = "created_by")
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
        if (status == null) {
            status = 0;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
