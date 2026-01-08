package com.camp.app.controller;

import com.camp.app.api.BaseResponse;
import com.camp.app.api.PageResult;
import com.camp.app.model.ExamScoreVo;
import com.camp.app.service.ExamScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/scores")
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
        return BaseResponse.ok(examScoreService.list(page, size, studentId, courseId, examScheduleId, semester, academicYear, examType));
    }

    @GetMapping("/{id}")
    public BaseResponse<ExamScoreVo> detail(@PathVariable Long id) {
        return BaseResponse.ok(examScoreService.detail(id));
    }

    @PostMapping
    public BaseResponse<Void> create(@RequestBody ExamScoreVo score) {
        examScoreService.create(score);
        return BaseResponse.ok(null);
    }

    @PutMapping("/{id}")
    public BaseResponse<Void> update(@PathVariable Long id, @RequestBody ExamScoreVo score) {
        examScoreService.update(id, score);
        return BaseResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable Long id) {
        examScoreService.delete(id);
        return BaseResponse.ok(null);
    }
}

