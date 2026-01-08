package com.camp.app.model;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ExamScoreVo {
    private Long id;
    private Long studentId;
    private Long courseId;
    private Long examScheduleId; // 关联考试安排ID
    private String examType;
    private Double score;
    private Double credit;
    private Double gradePoint;
    private LocalDate examDate;
    private String semester;
    private String academicYear;
    private String remark;
    private String remarkDetail; // 备注详情（记录如考试缺席等原因）
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

