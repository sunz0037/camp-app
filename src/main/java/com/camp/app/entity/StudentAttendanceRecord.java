package com.camp.app.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_attendance_records", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"student_id", "attendance_date"})
})
@Data
public class StudentAttendanceRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private LocalDate attendanceDate;

    @Column(nullable = false)
    private Integer status; // 1-缺勤，2-迟到，3-请假，4-早退

    @Column(length = 50)
    private String reasonType; // 生病、参加比赛等

    @Column(length = 500)
    private String reasonDescription;

    @Column(name = "parent_notified")
    private Boolean parentNotified = false;

    @Column(name = "parent_notify_time")
    private LocalDateTime parentNotifyTime;

    @Column(name = "parent_confirm_time")
    private LocalDateTime parentConfirmTime;

    @Column(length = 500)
    private String attachmentUrl;

    @Column(name = "recorded_by")
    private Long recordedBy;

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

