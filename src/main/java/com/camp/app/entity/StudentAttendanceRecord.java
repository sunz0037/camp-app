package com.camp.app.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "edu_attendance_records")
@Data
public class StudentAttendanceRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "summary_id")
    private Long summaryId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "course_id")
    private Long courseId;

    /** 1 LATE 2 EARLY_LEAVE 3 PERSONAL_LEAVE 4 SICK_LEAVE 5 ABSENT */
    @Column(name = "exception_type", nullable = false)
    private Integer exceptionType;

    /** 1 PENDING 2 CONFIRMED 3 CLEARED */
    @Column(name = "confirm_status", nullable = false)
    private Integer confirmStatus = 2;

    @Column(name = "reason_type", length = 30)
    private String reasonType;

    @Column(name = "reason_detail", length = 255)
    private String reasonDetail;

    @Column(length = 20)
    private String grade;

    @Column(name = "class_name", length = 50)
    private String className;

    @Column(name = "notified_contact_id")
    private Long notifiedContactId;

    @Column(name = "notified_contact_name", length = 50)
    private String notifiedContactName;

    @Column(name = "notified_method", length = 30)
    private String notifiedMethod;

    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;

    @Column(length = 255)
    private String attachmentUrl;

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
        if (confirmStatus == null) {
            confirmStatus = 2;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
