package com.camp.app.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CourseVo {
    private Long id;
    private String courseCode;
    private String courseName;
    private Double credit;
    private String courseType;
    private String department;
    private Long teacherId;
    private String semester;
    private String academicYear;
    private String description;
    /**
     * 0-停用，1-启用
     */
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

