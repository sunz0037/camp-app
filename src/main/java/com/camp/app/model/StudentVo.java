package com.camp.app.model;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class StudentVo {
    private Long id;
    private Long userId;
    private String studentNo;
    
    // 保留冗余字段用于显示
    private String grade;
    private String className;
    
    // 新增：外键ID字段
    private Long gradeId;
    private Long classId;
    
    // 新增：关联对象信息（可选，用于详情查询）
    private String gradeName; // 年级名称
    private String classNameFull; // 完整班级名称
    private String classCode; // 班级代码
    
    private String major;
    private String department;
    private LocalDate enrollmentDate;
    private LocalDate graduationDate;
    private String dormitory;
    private String address;
    private String politicalStatus;
    private String nationality;
    private Integer gender;
    private LocalDate birthDate;
    /**
     * 0-休学，1-在读，2-毕业，3-退学
     */
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

