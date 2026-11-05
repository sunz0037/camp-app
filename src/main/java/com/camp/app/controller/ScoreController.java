package com.camp.app.controller;

import com.camp.app.api.BaseResponse;
import com.camp.app.api.PageResult;
import com.camp.app.model.ExamScoreVo;
import com.camp.app.security.AuthContext;
import com.camp.app.security.Phase1Permissions;
import com.camp.app.security.RequirePermission;
import com.camp.app.service.ExamScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@ConditionalOnProperty(name = "camp.phase2.exam.enabled", havingValue = "true")
@RequestMapping("/api/v1/scores")
public class ScoreController {

    private final ExamScoreService examScoreService;

    @GetMapping
    public BaseResponse<PageResult<ExamScoreVo>> list(@RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "20") long size,
                                                    @RequestParam(required = false) Long studentId,
                                                    @RequestParam(required = false) Long courseId,
                                                    @RequestParam(required = false) Long examScheduleId,
                                                    @RequestParam(required = false) String semester,
                                                    @RequestParam(required = false) String academicYear,
                                                    @RequestParam(required = false) String examType) {
        return BaseResponse.ok(examScoreService.list(page, size, studentId, courseId, examScheduleId, semester, academicYear, examType, null, null, null));
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

