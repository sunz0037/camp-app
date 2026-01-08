package com.camp.app.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GradeAdminGradeVo {
    private Long id;
    private Long adminId;
    private String grade;
    private String department;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

