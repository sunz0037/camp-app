package com.camp.app.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 年级VO
 */
@Data
public class GradeVo {
    private Long id;
    private String gradeCode;
    private String gradeName;
    private String gradeType; // PRIMARY-小学，JUNIOR-初中，SENIOR-高中
    private Integer gradeLevel; // 1-6（小学），1-3（初中），1-3（高中）
    private String description;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

