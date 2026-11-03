package com.camp.app.model;

import lombok.Data;

@Data
public class SessionSlot {
    private Integer weekday;
    private Integer periodStart;
    private Integer periodEnd;
    private String roomNo;
}
