package com.camp.app.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "exam_scores", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"student_id", "course_id", "exam_type", "semester", "academic_year"})
})
@Data
public class ExamScore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "exam_schedule_id")
    private Long examScheduleId;

    @Column(nullable = false, length = 20)
    private String examType;

    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    @Column(precision = 3, scale = 1)
    private BigDecimal credit;

    @Column(precision = 3, scale = 2)
    private BigDecimal gradePoint;

    private LocalDate examDate;

    @Column(length = 20)
    private String semester;

    @Column(length = 20)
    private String academicYear;

    @Column(length = 255)
    private String remark;

    @Column(name = "remark_detail", columnDefinition = "TEXT")
    private String remarkDetail; // 备注详情（记录如考试缺席等原因）

    @Column(name = "created_by")
    private Long createdBy;

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

