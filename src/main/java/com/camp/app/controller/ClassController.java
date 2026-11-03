package com.camp.app.controller;

import com.camp.app.api.BaseResponse;
import com.camp.app.api.PageResult;
import com.camp.app.api.Paging;
import com.camp.app.model.ClassVo;
import com.camp.app.model.OptionItem;
import com.camp.app.security.Phase1Permissions;
import com.camp.app.security.RequirePermission;
import com.camp.app.service.ClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 班级管理控制器
 */
@RestController
@RequestMapping("/api/v1/classes")
@RequiredArgsConstructor
public class ClassController {
    
    private final ClassService classService;
    
    /**
     * 分页查询班级列表
     */
    @GetMapping
    public BaseResponse<PageResult<ClassVo>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(name = "pageSize", required = false) Long pageSize,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Long gradeId,
            @RequestParam(required = false) String gradeType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        return BaseResponse.ok(classService.list(Paging.page(page), Paging.size(pageSize, size), gradeId, gradeType, status, keyword));
    }

    @GetMapping("/options")
    public BaseResponse<java.util.List<OptionItem>> options(@RequestParam(required = false) Long gradeId) {
        return BaseResponse.ok(classService.options(gradeId));
    }
    
    /**
     * 查询班级详情
     */
    @GetMapping("/{id}")
    public BaseResponse<ClassVo> detail(@PathVariable Long id) {
        return BaseResponse.ok(classService.detail(id));
    }
    
    /**
     * 创建班级
     */
    @PostMapping
    @RequirePermission(Phase1Permissions.ORG_WRITE)
    public BaseResponse<Void> create(@RequestBody ClassVo classVo) {
        classService.create(classVo);
        return BaseResponse.ok(null);
    }
    
    /**
     * 更新班级
     */
    @PutMapping("/{id}")
    @RequirePermission(Phase1Permissions.ORG_WRITE)
    public BaseResponse<Void> update(@PathVariable Long id, @RequestBody ClassVo classVo) {
        classService.update(id, classVo);
        return BaseResponse.ok(null);
    }
    
    /**
     * 删除班级
     */
    @DeleteMapping("/{id}")
    @RequirePermission(Phase1Permissions.ORG_WRITE)
    public BaseResponse<Void> delete(@PathVariable Long id) {
        classService.delete(id);
        return BaseResponse.ok(null);
    }
    
    /**
     * 根据年级ID查询班级列表
     */
    @GetMapping("/by-grade/{gradeId}")
    public BaseResponse<List<ClassVo>> listByGradeId(@PathVariable Long gradeId) {
        return BaseResponse.ok(classService.listByGradeId(gradeId));
    }
    
    /**
     * 根据年级ID查询启用的班级列表
     */
    @GetMapping("/by-grade/{gradeId}/enabled")
    public BaseResponse<List<ClassVo>> listEnabledByGradeId(@PathVariable Long gradeId) {
        return BaseResponse.ok(classService.listEnabledByGradeId(gradeId));
    }
}

