package com.camp.app.model;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class StudentAttendanceRecordVo {
    private Long id;
    private Long studentId;
    private LocalDate attendanceDate;
    private String grade;
    private String className;
    /**
     * 1-缺勤，2-迟到，3-请假，4-早退（仅记录异常）
     */
    private Integer status;
    private String reasonType;
    private String reasonDetail;
    private Long notifiedContactId;
    private String notifiedContactName;
    private String notifiedMethod;
    private LocalDateTime notifiedAt;
    private String attachmentUrl;
    private Long recorderId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

