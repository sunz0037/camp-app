package com.camp.app.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EmergencyContactVo {
    private Long id;
    private Long studentId;
    private String contactName;
    private String relationship;
    private String phone;
    private String alternatePhone;
    private String address;
    /**
     * 0-否，1-是
     */
    private Integer isPrimary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

