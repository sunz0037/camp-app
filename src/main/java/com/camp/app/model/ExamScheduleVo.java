package com.camp.app.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * 考试安排。落在 edu_exams 的列只有名称、班级、课程、日期、学年、学期。
 * name / startTime / subject 是前端别名，不另存一套字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamScheduleVo {

    private Long id;

    private String examName;

    private Long classId;

    private String className;

    private String gradeName;

    private Long courseId;

    /** 课程名称。请求里的 subject 写入这里，再解析成 courseId。 */
    private String courseName;

    private String academicYear;

    /** 1 或 2，对应 edu_exams.semester */
    private Integer semester;

    private LocalDateTime examDate;

    private String targetGrade;

    private String examType;

    private Boolean isPublic;

    private String remark;

    private Long createdBy;

    private String createdByName;

    /**
     * 请求里的取消标记。edu_exams 没有状态列，响应保持空。
     * NORMAL / 1 忽略；CANCELLED / 0 拒绝写入。
     */
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @JsonAlias({"name", "title"})
    public void setExamName(String examName) {
        this.examName = examName;
    }

    @JsonAlias({"startTime", "startAt", "beginTime"})
    public void setExamDate(LocalDateTime examDate) {
        this.examDate = examDate;
    }

    @JsonAlias("subject")
    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public boolean requestsCancel() {
        return cancelledToken(status);
    }

    public static boolean cancelledToken(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        String text = status.trim();
        String upper = text.toUpperCase(Locale.ROOT);
        return "0".equals(upper) || "CANCELLED".equals(upper) || "CANCELED".equals(upper) || "已取消".equals(text);
    }

    public static boolean normalToken(String status) {
        if (status == null || status.isBlank()) {
            return true;
        }
        String text = status.trim();
        String upper = text.toUpperCase(Locale.ROOT);
        return "1".equals(upper) || "NORMAL".equals(upper) || "正常".equals(text);
    }
}
