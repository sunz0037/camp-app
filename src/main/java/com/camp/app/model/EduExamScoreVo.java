package com.camp.app.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class EduExamScoreVo {
    private Long id;
    private Long examId;
    private String examName;
    private Long studentId;
    private String studentNo;
    private String studentName;
    private Long classId;
    private String className;
    private Long gradeId;
    private BigDecimal score;
    private String gradeLevel;
    private Long recordedBy;
    private String recordedByName;
    private LocalDateTime recordedAt;
    private String remark;
}
