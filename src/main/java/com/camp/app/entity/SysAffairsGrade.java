package com.camp.app.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教务-年级授权。对应契约中的 sys_affairs_grades。
 * DDL 以数据库任务为准（预期 db/phase1/）。
 */
@Data
@Entity
@Table(name = "sys_affairs_grades", uniqueConstraints = @UniqueConstraint(name = "uk_sys_affairs_grade", columnNames = {"user_id", "grade_id"}))
public class SysAffairsGrade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "grade_id", nullable = false)
    private Long gradeId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
