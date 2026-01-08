package com.camp.app.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "courses")
@Data
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String courseCode;

    @Column(nullable = false, length = 100)
    private String courseName;

    @Column(nullable = false, precision = 3, scale = 1)
    private BigDecimal credit;

    @Column(length = 20)
    private String courseType;

    @Column(length = 100)
    private String department;

    @Column(name = "teacher_id")
    private Long teacherId;

    @Column(length = 20)
    private String semester;

    @Column(length = 20)
    private String academicYear;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer status = 1;

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

