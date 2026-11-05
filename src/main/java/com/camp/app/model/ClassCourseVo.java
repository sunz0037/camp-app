package com.camp.app.model;

import com.camp.app.util.FlexibleStringDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

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
    @JsonDeserialize(using = FlexibleStringDeserializer.class)
    private String semester;
    private List<SessionSlot> sessions;
    private String academicYear;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

