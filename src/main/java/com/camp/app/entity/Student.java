package com.camp.app.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "students", indexes = {
    @Index(name = "idx_class_id", columnList = "class_id"),
    @Index(name = "idx_grade_id", columnList = "grade_id")
})
@Data
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, unique = true, length = 20)
    private String studentNo;

    @Column(length = 20)
    private String grade; // 冗余字段，通过触发器或应用层同步

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grade_id")
    private Grade gradeEntity; // 新增：年级实体关联

    @Column(name = "grade_id", insertable = false, updatable = false)
    private Long gradeId; // 与 gradeEntity 共用列，避免重复映射

    @Column(length = 50)
    private String className; // 冗余字段，通过触发器或应用层同步

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private Class classEntity; // 新增：班级实体关联

    @Column(name = "class_id", insertable = false, updatable = false)
    private Long classId; // 与 classEntity 共用列，避免重复映射

    @Column(length = 100)
    private String major;

    @Column(length = 100)
    private String department;

    private LocalDate enrollmentDate;
    private LocalDate graduationDate;

    @Column(length = 50)
    private String dormitory;

    @Column(length = 255)
    private String address;

    @Column(length = 20)
    private String politicalStatus;

    @Column(length = 50)
    private String nationality = "中国";

    private Integer gender; // 0-女，1-男

    private LocalDate birthDate;

    @Column(nullable = false)
    private Integer status = 1; // 0-休学，1-在读，2-毕业，3-退学

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        // 如果设置了 classEntity，同步冗余字段
        if (classEntity != null && classId == null) {
            classId = classEntity.getId();
            className = classEntity.getClassName();
            if (classEntity.getGrade() != null) {
                gradeEntity = classEntity.getGrade();
                gradeId = gradeEntity.getId();
                grade = gradeEntity.getGradeName();
            }
        }
        // 如果设置了 gradeEntity，同步冗余字段
        if (gradeEntity != null && gradeId == null) {
            gradeId = gradeEntity.getId();
            grade = gradeEntity.getGradeName();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        // 同步冗余字段
        if (classEntity != null) {
            classId = classEntity.getId();
            className = classEntity.getClassName();
            if (classEntity.getGrade() != null) {
                gradeEntity = classEntity.getGrade();
                gradeId = gradeEntity.getId();
                grade = gradeEntity.getGradeName();
            }
        } else if (gradeEntity != null) {
            gradeId = gradeEntity.getId();
            grade = gradeEntity.getGradeName();
        }
    }
}

