package com.camp.app.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EduExamVo {
    private Long id;
    private String examCode;
    private String examName;
    private Long gradeId;
    private String gradeName;
    private Long classId;
    private String className;
    private Long courseId;
    private String subjectName;
    /** MIDTERM | FINAL | MONTHLY | QUIZ */
    private String examType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    @JsonProperty("isPublic")
    private Boolean isPublic;
    /** SCHEDULED | FINISHED | CANCELED */
    private String status;
    private String roomNo;
    private String remark;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
