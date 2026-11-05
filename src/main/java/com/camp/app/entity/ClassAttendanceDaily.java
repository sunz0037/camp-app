package com.camp.app.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "edu_attendance_summaries", uniqueConstraints = {
    @UniqueConstraint(name = "uk_class_id_attendance_date", columnNames = {"class_id", "attendance_date"})
})
@Data
public class ClassAttendanceDaily {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate attendanceDate;

    @Column(length = 20)
    private String grade;

    @Column(length = 50)
    private String className;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(nullable = false)
    private Integer expectedCount = 0;

    @Column(name = "present_count", nullable = false)
    private Integer presentCount = 0;

    @Column(nullable = false)
    private Integer absentCount = 0;

    @Column(nullable = false)
    private Integer lateCount = 0;

    @Column(nullable = false)
    private Integer leaveCount = 0;

    @Column(name = "early_leave_count", nullable = false)
    private Integer earlyLeaveCount = 0;

    @Column(name = "recorder_id")
    private Long recorderId;

    @Column(length = 255)
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
