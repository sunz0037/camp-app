package com.camp.app.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 考试安排实体类
 */
@Entity
@Table(name = "exam_schedules")
@Data
public class ExamSchedule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "exam_name", nullable = false, length = 100)
    private String examName;
    
    @Column(name = "exam_date", nullable = false)
    private LocalDateTime examDate;
    
    @Column(name = "target_grade", nullable = false, length = 20)
    private String targetGrade;
    
    @Column(name = "exam_type", nullable = false, length = 20)
    private String examType; // 月考、期中、期末
    
    @Column(name = "is_public", nullable = false)
    private Boolean isPublic; // 是否公开：false-不公开，true-公开
    
    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;
    
    @Column(name = "created_by", nullable = false)
    private Long createdBy;
    
    @Column(name = "status", nullable = false)
    private Integer status; // 0-已取消，1-正常
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = 1; // 默认正常状态
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

