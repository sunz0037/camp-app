package com.camp.app.model;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ClassAttendanceDailyVo {
    private Long id;
    private LocalDate attendanceDate;
    private String grade;
    private String className;
    private Integer expectedCount;
    private Integer presentCount;
    private Integer absentCount;
    private Integer lateCount;
    private Integer leaveCount;
    private Long recorderId;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

