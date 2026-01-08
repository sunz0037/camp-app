package com.camp.app.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 年级实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "grades", indexes = {
    @Index(name = "idx_grade_type", columnList = "grade_type"),
    @Index(name = "idx_grade_level", columnList = "grade_level"),
    @Index(name = "idx_status", columnList = "status")
})
public class Grade {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "grade_code", nullable = false, unique = true, length = 20)
    private String gradeCode;
    
    @Column(name = "grade_name", nullable = false, length = 50)
    private String gradeName;
    
    @Column(name = "grade_type", nullable = false, length = 20)
    private String gradeType; // PRIMARY-小学，JUNIOR-初中，SENIOR-高中
    
    @Column(name = "grade_level", nullable = false)
    private Integer gradeLevel; // 1-6（小学），1-3（初中），1-3（高中）
    
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
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

