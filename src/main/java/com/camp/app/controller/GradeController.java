package com.camp.app.controller;

import com.camp.app.api.BaseResponse;
import com.camp.app.api.PageResult;
import com.camp.app.model.GradeVo;
import com.camp.app.service.GradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 年级管理控制器
 */
@RestController
@RequestMapping("/api/grades")
@RequiredArgsConstructor
public class GradeController {
    
    private final GradeService gradeService;
    
    /**
     * 分页查询年级列表
     */
    @GetMapping
    public BaseResponse<PageResult<GradeVo>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String gradeType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        return BaseResponse.ok(gradeService.list(page, size, gradeType, status, keyword));
    }
    
    /**
     * 查询年级详情
     */
    @GetMapping("/{id}")
    public BaseResponse<GradeVo> detail(@PathVariable Long id) {
        return BaseResponse.ok(gradeService.detail(id));
    }
    
    /**
     * 创建年级
     */
    @PostMapping
    public BaseResponse<Void> create(@RequestBody GradeVo grade) {
        gradeService.create(grade);
        return BaseResponse.ok(null);
    }
    
    /**
     * 更新年级
     */
    @PutMapping("/{id}")
    public BaseResponse<Void> update(@PathVariable Long id, @RequestBody GradeVo grade) {
        gradeService.update(id, grade);
        return BaseResponse.ok(null);
    }
    
    /**
     * 删除年级
     */
    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable Long id) {
        gradeService.delete(id);
        return BaseResponse.ok(null);
    }
    
    /**
     * 查询所有启用的年级（不分页）
     */
    @GetMapping("/all-enabled")
    public BaseResponse<List<GradeVo>> listAllEnabled() {
        return BaseResponse.ok(gradeService.listAllEnabled());
    }
    
    /**
     * 根据年级类型查询
     */
    @GetMapping("/by-type/{gradeType}")
    public BaseResponse<List<GradeVo>> listByType(@PathVariable String gradeType) {
        return BaseResponse.ok(gradeService.listByType(gradeType));
    }
}

