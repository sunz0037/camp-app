package com.camp.app.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TimetableItemVo {
    private Long classCourseId;
    private Long classId;
    private String className;
    private Long courseId;
    private String courseName;
    private Long teacherId;
    private String teacherName;
    private String academicYear;
    private String semester;
    private Integer weekday;
    private Integer periodStart;
    private Integer periodEnd;
    private String roomNo;
}
