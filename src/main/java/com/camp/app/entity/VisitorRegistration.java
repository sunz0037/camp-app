package com.camp.app.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "visitor_registrations")
@Data
public class VisitorRegistration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String visitorName;

    @Column(nullable = false, length = 20)
    private String visitorPhone;

    @Column(length = 18)
    private String visitorIdCard;

    @Column(nullable = false, length = 200)
    private String visitPurpose;

    @Column(name = "visit_target_user_id")
    private Long visitTargetUserId;

    @Column(length = 50)
    private String visitTargetName;

    @Column(nullable = false)
    private LocalDate visitDate;

    private LocalDateTime visitTimeStart;
    private LocalDateTime visitTimeEnd;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;

    @Column(length = 100)
    private String vehicleInfo;

    @Column(nullable = false)
    private Integer accompanyCount = 0;

    @Column(name = "receptionist_id")
    private Long receptionistId;

    @Column(nullable = false)
    private Integer status = 0; // 0-待审核，1-已批准，2-已进入，3-已离开，4-已拒绝

    @Column(length = 500)
    private String remark;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

