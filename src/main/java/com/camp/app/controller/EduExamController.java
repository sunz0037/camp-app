package com.camp.app.controller;

import com.camp.app.api.BaseResponse;
import com.camp.app.api.BizException;
import com.camp.app.api.PageResult;
import com.camp.app.api.Paging;
import com.camp.app.model.EduExamVo;
import com.camp.app.service.EduExamService;
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
@RequestMapping("/api/v1/exams")
@RequiredArgsConstructor
public class EduExamController {
    private final EduExamService eduExamService;

    @GetMapping
    public BaseResponse<PageResult<EduExamVo>> list(@RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(name = "pageSize", required = false) Long pageSize,
                                                    @RequestParam(defaultValue = "20") long size,
                                                    @RequestParam(required = false) Long gradeId,
                                                    @RequestParam(required = false) Long classId,
                                                    @RequestParam(required = false) String examType,
                                                    @RequestParam(required = false) String isPublic,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(required = false) String keyword) {
        return BaseResponse.ok(eduExamService.list(
                Paging.page(page), Paging.size(pageSize, size), gradeId, classId, examType, flag(isPublic), status, keyword));
    }

    @GetMapping("/{id}")
    public BaseResponse<EduExamVo> detail(@PathVariable Long id) {
        return BaseResponse.ok(eduExamService.get(id));
    }

    @PostMapping
    public BaseResponse<EduExamVo> create(@RequestBody EduExamVo vo) {
        return BaseResponse.ok(eduExamService.create(vo));
    }

    @PutMapping("/{id}")
    public BaseResponse<EduExamVo> update(@PathVariable Long id, @RequestBody EduExamVo vo) {
        return BaseResponse.ok(eduExamService.update(id, vo));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable Long id) {
        eduExamService.delete(id);
        return BaseResponse.ok(null);
    }

    private Boolean flag(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim().toLowerCase();
        if ("true".equals(value) || "1".equals(value)) {
            return true;
        }
        if ("false".equals(value) || "0".equals(value)) {
            return false;
        }
        throw BizException.badRequest("isPublic 只能是 true 或 false");
    }
}
