package com.camp.app.controller;

import com.camp.app.api.BaseResponse;
import com.camp.app.api.PageResult;
import com.camp.app.api.Paging;
import com.camp.app.model.EduExamScoreVo;
import com.camp.app.service.EduExamScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/exam-scores")
@RequiredArgsConstructor
public class EduExamScoreController {
    private final EduExamScoreService eduExamScoreService;

    @GetMapping
    public BaseResponse<PageResult<EduExamScoreVo>> list(@RequestParam(defaultValue = "1") long page,
                                                         @RequestParam(name = "pageSize", required = false) Long pageSize,
                                                         @RequestParam(defaultValue = "20") long size,
                                                         @RequestParam(required = false) Long examId,
                                                         @RequestParam(required = false) Long studentId,
                                                         @RequestParam(required = false) Long classId,
                                                         @RequestParam(required = false) String keyword) {
        return BaseResponse.ok(eduExamScoreService.list(
                Paging.page(page), Paging.size(pageSize, size), examId, studentId, classId, keyword));
    }

    @GetMapping("/{id}")
    public BaseResponse<EduExamScoreVo> detail(@PathVariable Long id) {
        return BaseResponse.ok(eduExamScoreService.get(id));
    }

    @PostMapping
    public BaseResponse<EduExamScoreVo> create(@RequestBody EduExamScoreVo vo) {
        return BaseResponse.ok(eduExamScoreService.create(vo));
    }

    @PutMapping("/{id}")
    public BaseResponse<EduExamScoreVo> update(@PathVariable Long id, @RequestBody EduExamScoreVo vo) {
        return BaseResponse.ok(eduExamScoreService.update(id, vo));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable Long id) {
        eduExamScoreService.delete(id);
        return BaseResponse.ok(null);
    }
}
