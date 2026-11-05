package com.camp.app.controller;

import com.camp.app.api.BaseResponse;
import com.camp.app.api.PageResult;
import com.camp.app.model.ExamScheduleVo;
import com.camp.app.service.ExamScheduleService;
import com.camp.app.security.AuthContext;
import com.camp.app.security.Phase1Permissions;
import com.camp.app.security.RequirePermission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

/**
 * 考试安排控制器
 */
@Slf4j
@RestController
@ConditionalOnProperty(name = "camp.phase2.exam.enabled", havingValue = "true")
@RequestMapping("/api/v1/exam-schedules")
@RequiredArgsConstructor
public class ExamScheduleController {
    
    private final ExamScheduleService examScheduleService;
    
    /**
     * 分页查询考试安排
     */
    @GetMapping
    public BaseResponse<PageResult<ExamScheduleVo>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String targetGrade,
            @RequestParam(required = false) String examType,
            @RequestParam(required = false) Boolean isPublic,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        PageResult<ExamScheduleVo> result = examScheduleService.list(
            page, size, targetGrade, examType, isPublic, status, keyword);
        return BaseResponse.ok(result);
    }
    
    /**
     * 根据ID查询考试安排
     */
    @GetMapping("/{id}")
    public BaseResponse<ExamScheduleVo> getById(@PathVariable Long id) {
        ExamScheduleVo vo = examScheduleService.getById(id);
        return BaseResponse.ok(vo);
    }
    
    /**
     * 创建考试安排
     */
    @PostMapping
    @RequirePermission(Phase1Permissions.COURSE_WRITE)
    public BaseResponse<ExamScheduleVo> create(@RequestBody ExamScheduleVo vo,
                                                @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        Long actorId = userId != null ? userId : AuthContext.userId();
        if (actorId == null) {
            return BaseResponse.error(401, "未登录");
        }
        return BaseResponse.ok(examScheduleService.create(vo, actorId));
    }
    
    /**
     * 更新考试安排
     */
    @PutMapping("/{id}")
    @RequirePermission(Phase1Permissions.COURSE_WRITE)
    public BaseResponse<ExamScheduleVo> update(@PathVariable Long id, @RequestBody ExamScheduleVo vo) {
        ExamScheduleVo updated = examScheduleService.update(id, vo);
        return BaseResponse.ok(updated);
    }
    
    /**
     * 删除考试安排
     */
    @DeleteMapping("/{id}")
    @RequirePermission(Phase1Permissions.COURSE_WRITE)
    public BaseResponse<Void> delete(@PathVariable Long id) {
        examScheduleService.delete(id);
        return BaseResponse.ok(null);
    }
    
    /**
     * 取消考试安排
     */
    @PutMapping("/{id}/cancel")
    @RequirePermission(Phase1Permissions.COURSE_WRITE)
    public BaseResponse<ExamScheduleVo> cancel(@PathVariable Long id) {
        ExamScheduleVo canceled = examScheduleService.cancel(id);
        return BaseResponse.ok(canceled);
    }
}

