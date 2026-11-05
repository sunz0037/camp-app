package com.camp.app.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 考试安排VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamScheduleVo {
    
    private Long id;
    
    private String examName;

    private Long classId;

    private Long courseId;

    private String academicYear;

    /** 1 或 2，对应 edu_exams.semester */
    private Integer semester;
    
    private LocalDateTime examDate;
    
    private String targetGrade;
    
    private String examType; // 月考、期中、期末
    
    private Boolean isPublic; // 是否公开
    
    private String remark;
    
    private Long createdBy;
    
    private String createdByName; // 创建人姓名
    
    private Integer status; // 0-已取消，1-正常
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}

