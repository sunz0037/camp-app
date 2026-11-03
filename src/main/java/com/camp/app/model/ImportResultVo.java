package com.camp.app.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ImportResultVo {
    private int successCount;
    private int failCount;
    private List<RowError> errors;

    @Data
    @Builder
    public static class RowError {
        private int row;
        private String message;
    }
}
