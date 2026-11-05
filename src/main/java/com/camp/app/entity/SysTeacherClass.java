package com.camp.app.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教师-班级授权。首期数据范围以本表为准，不使用 scope_json。
 * 物理表名 sys_teacher_classes，DDL 以数据库任务为准（预期 db/phase1/）。
 */
@Data
@Entity
@Table(name = "sys_teacher_classes", uniqueConstraints = @UniqueConstraint(name = "uk_sys_teacher_class", columnNames = {"teacher_id", "class_id"}))
public class SysTeacherClass {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
