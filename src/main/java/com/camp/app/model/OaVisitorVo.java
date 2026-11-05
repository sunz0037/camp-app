package com.camp.app.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OaVisitorVo {
    private Long id;
    private String visitorName;
    private String phone;
    private String idCard;
    private String org;
    private String purpose;
    private Long hostUserId;
    private String hostName;
    private String hostDept;
    private LocalDateTime appointmentTime;
    private LocalDateTime expectedLeave;
    private String status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
