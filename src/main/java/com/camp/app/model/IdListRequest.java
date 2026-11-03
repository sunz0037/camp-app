package com.camp.app.model;

import lombok.Data;

import java.util.List;

@Data
public class IdListRequest {
    private List<Long> classIds;
    private List<Long> gradeIds;
}
