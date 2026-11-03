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
@Table(name = "edu_exams")
public class EduExam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exam_code", nullable = false, unique = true, length = 40)
    private String examCode;

    @Column(name = "exam_name", nullable = false, length = 100)
    private String examName;

    @Column(name = "grade_id")
    private Long gradeId;

    @Column(name = "class_id")
    private Long classId;

    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "subject_name", length = 100)
    private String subjectName;

    @Column(name = "exam_type", nullable = false, length = 20)
    private String examType;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "is_public", nullable = false)
    private Integer isPublic;

    @Column(nullable = false)
    private Integer status;

    @Column(name = "room_no", length = 32)
    private String roomNo;

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
        if (isPublic == null) {
            isPublic = 0;
        }
        if (status == null) {
            status = 1;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
