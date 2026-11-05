package com.camp.app.model;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class AttendanceSummaryRequest {
    private Long classId;
    private LocalDate attendanceDate;
    private Integer expectedCount;
    private Integer actualCount;
    private String remark;
    private List<AttendanceRecordItem> records;
}
