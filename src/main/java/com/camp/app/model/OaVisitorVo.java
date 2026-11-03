package com.camp.app.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OaVisitorVo {
    private Long id;
    private String visitNo;
    private String visitorName;
    private String idCard;
    private String phone;
    private String purpose;
    private Long hostUserId;
    private String hostName;
    private LocalDateTime appointmentTime;
    private LocalDateTime arrivalTime;
    private LocalDateTime leaveTime;
    /** PENDING | APPROVED | REJECTED | CHECKED_IN | CHECKED_OUT */
    private String status;
    private String remark;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
