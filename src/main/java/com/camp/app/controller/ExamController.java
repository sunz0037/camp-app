package com.camp.app.controller;

import com.camp.app.api.BaseResponse;
import com.camp.app.api.BizException;
import com.camp.app.api.PageResult;
import com.camp.app.api.Paging;
import com.camp.app.model.ExamScheduleVo;
import com.camp.app.security.AuthContext;
import com.camp.app.security.Phase1Permissions;
import com.camp.app.security.RequirePermission;
import com.camp.app.service.ExamAliasAdapter;
import com.camp.app.service.ExamScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前端考试安排页使用的路径。读写仍走 {@link ExamScheduleService} 和 edu_exams。
 */
@RestController
@ConditionalOnProperty(name = "camp.phase2.exam.enabled", havingValue = "true")
@RequestMapping("/api/v1/exams")
@RequiredArgsConstructor
public class ExamController {
    private final ExamScheduleService examScheduleService;
    private final ExamAliasAdapter examAliasAdapter;

    @GetMapping
    public BaseResponse<PageResult<ExamScheduleVo>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(name = "pageSize", required = false) Long pageSize,
            @RequestParam(required = false) Long size,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) String examType,
            @RequestParam(required = false) String isPublic,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Integer semester) {
        long pageNo = Paging.page(page);
        long pageSizeValue = Paging.size(pageSize, size == null ? 20 : size);
        if (hasText(examType)) {
            throw BizException.badRequest(ExamScheduleService.UNSUPPORTED_FILTER);
        }
        Boolean publicFlag = publicFlag(isPublic);
        if (Boolean.TRUE.equals(publicFlag)) {
            return BaseResponse.ok(empty(pageNo, pageSizeValue));
        }
        if (ExamScheduleVo.cancelledToken(status)) {
            return BaseResponse.ok(empty(pageNo, pageSizeValue));
        }
        if (!ExamScheduleVo.normalToken(status)) {
            throw BizException.badRequest(ExamScheduleService.UNSUPPORTED_FILTER);
        }
        return BaseResponse.ok(examScheduleService.list(
                (int) pageNo, (int) pageSizeValue, null, null, null, null, keyword,
                grade, classId, courseId, academicYear, semester));
    }

    @GetMapping("/{id}")
    public BaseResponse<ExamScheduleVo> getById(@PathVariable Long id) {
        return BaseResponse.ok(examScheduleService.getById(id));
    }

    @PostMapping
    @RequirePermission(Phase1Permissions.COURSE_WRITE)
    public BaseResponse<ExamScheduleVo> create(@RequestBody ExamScheduleVo vo,
                                               @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        Long actorId = userId != null ? userId : AuthContext.userId();
        if (actorId == null) {
            return BaseResponse.error(401, "未登录");
        }
        examAliasAdapter.prepareCreate(vo);
        return BaseResponse.ok(examScheduleService.create(vo, actorId));
    }

    @PutMapping("/{id}")
    @RequirePermission(Phase1Permissions.COURSE_WRITE)
    public BaseResponse<ExamScheduleVo> update(@PathVariable Long id, @RequestBody ExamScheduleVo vo) {
        examAliasAdapter.prepareUpdate(vo);
        return BaseResponse.ok(examScheduleService.update(id, vo));
    }

    @DeleteMapping("/{id}")
    @RequirePermission(Phase1Permissions.COURSE_WRITE)
    public BaseResponse<Void> delete(@PathVariable Long id) {
        examScheduleService.delete(id);
        return BaseResponse.ok(null);
    }

    @PutMapping("/{id}/cancel")
    @RequirePermission(Phase1Permissions.COURSE_WRITE)
    public BaseResponse<ExamScheduleVo> cancel(@PathVariable Long id) {
        return BaseResponse.ok(examScheduleService.cancel(id));
    }

    private PageResult<ExamScheduleVo> empty(long page, long size) {
        return PageResult.<ExamScheduleVo>builder().page(page).size(size).total(0).records(List.of()).build();
    }

    private Boolean publicFlag(String raw) {
        if (!hasText(raw)) {
            return null;
        }
        String text = raw.trim();
        if ("true".equalsIgnoreCase(text) || "1".equals(text)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(text) || "0".equals(text)) {
            return Boolean.FALSE;
        }
        throw BizException.badRequest(ExamScheduleService.UNSUPPORTED_FILTER);
    }

    private boolean hasText(String raw) {
        return raw != null && !raw.isBlank();
    }
}
