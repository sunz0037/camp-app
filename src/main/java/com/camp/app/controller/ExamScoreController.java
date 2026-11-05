package com.camp.app.controller;

import com.camp.app.api.BaseResponse;
import com.camp.app.api.BizException;
import com.camp.app.api.PageResult;
import com.camp.app.api.Paging;
import com.camp.app.model.ExamScoreVo;
import com.camp.app.security.Phase1Permissions;
import com.camp.app.security.RequirePermission;
import com.camp.app.service.ExamScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 前端考试成绩页使用的路径。读写仍走 {@link ExamScoreService} 和 edu_exam_scores。
 */
@RestController
@ConditionalOnProperty(name = "camp.phase2.exam.enabled", havingValue = "true")
@RequestMapping("/api/v1/exam-scores")
@RequiredArgsConstructor
public class ExamScoreController {
    private final ExamScoreService examScoreService;

    @GetMapping
    public BaseResponse<PageResult<ExamScoreVo>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(name = "pageSize", required = false) Long pageSize,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long examScheduleId,
            @RequestParam(required = false) Long examId,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) String examType,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) String className,
            @RequestParam(required = false) String keyword) {
        if (examScheduleId != null && examId != null && !examScheduleId.equals(examId)) {
            throw BizException.badRequest("examId 与 examScheduleId 不一致");
        }
        Long exam = examScheduleId != null ? examScheduleId : examId;
        return BaseResponse.ok(examScoreService.list(
                Paging.page(page), Paging.size(pageSize, size),
                studentId, courseId, exam, semester, academicYear, examType, classId, className, keyword));
    }

    @GetMapping("/{id}")
    public BaseResponse<ExamScoreVo> detail(@PathVariable Long id) {
        return BaseResponse.ok(examScoreService.detail(id));
    }

    @PostMapping
    @RequirePermission(Phase1Permissions.COURSE_WRITE)
    public BaseResponse<ExamScoreVo> create(@RequestBody ExamScoreVo score) {
        return BaseResponse.ok(examScoreService.create(score));
    }

    @PutMapping("/{id}")
    @RequirePermission(Phase1Permissions.COURSE_WRITE)
    public BaseResponse<ExamScoreVo> update(@PathVariable Long id, @RequestBody ExamScoreVo score) {
        return BaseResponse.ok(examScoreService.update(id, score));
    }

    @DeleteMapping("/{id}")
    @RequirePermission(Phase1Permissions.COURSE_WRITE)
    public BaseResponse<Void> delete(@PathVariable Long id) {
        examScoreService.delete(id);
        return BaseResponse.ok(null);
    }
}
