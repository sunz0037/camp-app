package com.camp.app.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ExamScoreVo {
    private Long id;
    private Long studentId;
    private String studentNo;
    private String studentName;
    private Long classId;
    private String className;
    private Long courseId;
    private String courseName;
    private String examName;
    /** 关联 edu_exams.id。请求里的 examId 是同一列的别名。 */
    private Long examScheduleId;
    private String examType;
    private Double score;
    /** 按分数换算的 A–E，只出现在响应里，不入库。 */
    private String gradeLevel;
    private Double credit;
    private Double gradePoint;
    private LocalDate examDate;
    private String semester;
    private String academicYear;
    private String remark;
    private String remarkDetail;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @JsonAlias("examId")
    public void setExamScheduleId(Long examScheduleId) {
        this.examScheduleId = examScheduleId;
    }
}

