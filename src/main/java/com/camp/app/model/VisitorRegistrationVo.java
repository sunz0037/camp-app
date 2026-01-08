package com.camp.app.model;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class VisitorRegistrationVo {
    private Long id;
    private String visitorName;
    private String visitorPhone;
    private String visitorIdCard;
    private String visitPurpose;
    private Long visitTargetUserId;
    private String visitTargetName;
    private LocalDate visitDate;
    private LocalDateTime visitTimeStart;
    private LocalDateTime visitTimeEnd;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private String vehicleInfo;
    private Integer accompanyCount;
    private Long receptionistId;
    /**
     * 0-待审核，1-已批准，2-已进入，3-已离开，4-已拒绝
     */
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

