package com.camp.app.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TeacherClassVo {
    private Long id;
    private Long teacherId;
    private String grade;
    private String className;
    private String subject;
    /**
     * 0-否，1-是
     */
    private Integer isClassTeacher;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

