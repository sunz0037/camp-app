package com.camp.app.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "class_attendance_daily", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"attendance_date", "grade", "class_name"})
})
@Data
public class ClassAttendanceDaily {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate attendanceDate;

    @Column(nullable = false, length = 20)
    private String grade;

    @Column(nullable = false, length = 50)
    private String className;

    /** 首期汇总按班级+日期，不挂 courseId。 */
    @Column(name = "class_id")
    private Long classId;

    @Column(name = "early_leave_count")
    private Integer earlyLeaveCount;

    @Column(nullable = false)
    private Integer expectedCount;

    @Column(nullable = false)
    private Integer actualCount;

    @Column(nullable = false)
    private Integer absentCount;

    @Column(nullable = false)
    private Integer lateCount;

    @Column(nullable = false)
    private Integer leaveCount;

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

