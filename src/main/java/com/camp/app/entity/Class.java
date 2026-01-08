package com.camp.app.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 班级实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "classes", indexes = {
    @Index(name = "idx_grade_id", columnList = "grade_id"),
    @Index(name = "idx_class_code", columnList = "class_code"),
    @Index(name = "idx_status", columnList = "status")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_grade_class", columnNames = {"grade_id", "class_number"})
})
public class Class {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grade_id", nullable = false)
    private Grade grade;
    
    @Column(name = "class_code", nullable = false, length = 50)
    private String classCode;
    
    @Column(name = "class_name", nullable = false, length = 50)
    private String className;
    
    @Column(name = "class_number", nullable = false)
    private Integer classNumber; // 班级序号（如：1班、2班）
    
    @Column(name = "capacity")
    private Integer capacity; // 班级容量（默认50人）
    
    @Column(name = "current_count")
    private Integer currentCount; // 当前学生数
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "head_teacher_id")
    private User headTeacher; // 班主任
    
    @Column(name = "description", length = 255)
    private String description;
    
    @Column(name = "status", nullable = false)
    private Integer status; // 0-停用，1-启用
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (capacity == null) {
            capacity = 50;
        }
        if (currentCount == null) {
            currentCount = 0;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

