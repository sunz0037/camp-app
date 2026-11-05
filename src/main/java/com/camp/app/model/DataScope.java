package com.camp.app.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DataScope {
    private String level;
    private List<Long> gradeIds;
    private List<Long> classIds;
    private Long studentId;
    private Long userId;

    public boolean isAll() {
        return "ALL".equals(level);
    }
}
