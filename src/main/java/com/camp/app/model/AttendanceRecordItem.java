package com.camp.app.model;

import lombok.Data;

@Data
public class AttendanceRecordItem {
    private Long studentId;
    private Long courseId;
    private String exceptionType;
    private String confirmStatus;
    private String reasonDescription;
}
