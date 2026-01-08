package com.camp.app.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 班级VO
 */
@Data
public class ClassVo {
    private Long id;
    private Long gradeId;
    private String gradeCode;
    private String gradeName;
    private String gradeType;
    private String classCode;
    private String className;
    private Integer classNumber;
    private Integer capacity;
    private Integer currentCount;
    private Long headTeacherId;
    private String headTeacherName;
    private String description;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

