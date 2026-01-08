package com.camp.app.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 班级授课科目关联VO
 */
@Data
public class ClassCourseVo {
    private Long id;
    private Long classId;
    private String className;
    private String classCode;
    private Long courseId;
    private String courseCode;
    private String courseName;
    private String courseType;
    private Long teacherId;
    private String teacherName;
    private String semester;
    private String academicYear;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

