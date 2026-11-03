package com.camp.app.controller;

import com.camp.app.api.BaseResponse;
import com.camp.app.api.PageResult;
import com.camp.app.model.ExamScheduleVo;
import com.camp.app.service.ExamScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

/**
 * 考试安排控制器
 */
@Slf4j
@RestController
@ConditionalOnProperty(name = "camp.phase2.enabled", havingValue = "true")
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
    public BaseResponse<ExamScheduleVo> create(@RequestBody ExamScheduleVo vo,
                                                @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        // 从请求头获取用户ID（前端会在请求头中传递）
        if (userId == null) {
            return BaseResponse.error(400, "用户ID不能为空，请先登录");
        }
        ExamScheduleVo created = examScheduleService.create(vo, userId);
        return BaseResponse.ok(created);
    }
    
    /**
     * 更新考试安排
     */
    @PutMapping("/{id}")
    public BaseResponse<ExamScheduleVo> update(@PathVariable Long id, @RequestBody ExamScheduleVo vo) {
        ExamScheduleVo updated = examScheduleService.update(id, vo);
        return BaseResponse.ok(updated);
    }
    
    /**
     * 删除考试安排
     */
    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable Long id) {
        examScheduleService.delete(id);
        return BaseResponse.ok(null);
    }
    
    /**
     * 取消考试安排
     */
    @PutMapping("/{id}/cancel")
    public BaseResponse<ExamScheduleVo> cancel(@PathVariable Long id) {
        ExamScheduleVo canceled = examScheduleService.cancel(id);
        return BaseResponse.ok(canceled);
    }
}

