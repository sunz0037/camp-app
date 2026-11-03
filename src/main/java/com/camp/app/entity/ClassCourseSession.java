package com.camp.app.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 班级授课的结构化节次。禁止再用纯文本 schedule_info 作为课表来源。
 * DDL 以数据库任务为准（预期 db/phase1/）。
 */
@Data
@Entity
@Table(name = "class_course_sessions", indexes = @Index(name = "idx_session_class_course", columnList = "class_course_id"))
public class ClassCourseSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_course_id", nullable = false)
    private Long classCourseId;

    @Column(nullable = false)
    private Integer weekday;

    @Column(name = "period_start", nullable = false)
    private Integer periodStart;

    @Column(name = "period_end", nullable = false)
    private Integer periodEnd;

    @Column(name = "room_no", length = 50)
    private String roomNo;
}
