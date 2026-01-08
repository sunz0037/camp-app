package com.camp.app.controller;

import com.camp.app.api.BaseResponse;
import com.camp.app.model.ClassCourseVo;
import com.camp.app.service.ClassCourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 班级授课科目关联控制器
 */
@RestController
@RequestMapping("/api/class-courses")
@RequiredArgsConstructor
public class ClassCourseController {
    
    private final ClassCourseService classCourseService;
    
    /**
     * 查询班级的所有授课科目
     */
    @GetMapping("/by-class/{classId}")
    public BaseResponse<List<ClassCourseVo>> listByClassId(@PathVariable Long classId) {
        List<ClassCourseVo> list = classCourseService.listByClassId(classId);
        return BaseResponse.ok(list);
    }
    
    /**
     * 查询班级的启用授课科目
     */
    @GetMapping("/by-class/{classId}/enabled")
    public BaseResponse<List<ClassCourseVo>> listEnabledByClassId(@PathVariable Long classId) {
        List<ClassCourseVo> list = classCourseService.listEnabledByClassId(classId);
        return BaseResponse.ok(list);
    }
    
    /**
     * 查询详情
     */
    @GetMapping("/{id}")
    public BaseResponse<ClassCourseVo> detail(@PathVariable Long id) {
        ClassCourseVo vo = classCourseService.detail(id);
        return BaseResponse.ok(vo);
    }
    
    /**
     * 添加班级授课科目
     */
    @PostMapping
    public BaseResponse<Void> create(@RequestBody ClassCourseVo classCourseVo) {
        classCourseService.create(classCourseVo);
        return BaseResponse.ok(null);
    }
    
    /**
     * 批量添加班级授课科目
     */
    @PostMapping("/batch")
    public BaseResponse<Void> batchCreate(@RequestParam Long classId, @RequestBody List<Long> courseIds) {
        classCourseService.batchCreate(classId, courseIds);
        return BaseResponse.ok(null);
    }
    
    /**
     * 更新班级授课科目
     */
    @PutMapping("/{id}")
    public BaseResponse<Void> update(@PathVariable Long id, @RequestBody ClassCourseVo classCourseVo) {
        classCourseService.update(id, classCourseVo);
        return BaseResponse.ok(null);
    }
    
    /**
     * 删除班级授课科目
     */
    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable Long id) {
        classCourseService.delete(id);
        return BaseResponse.ok(null);
    }
    
    /**
     * 批量删除班级授课科目
     */
    @DeleteMapping("/batch")
    public BaseResponse<Void> batchDelete(@RequestBody List<Long> ids) {
        classCourseService.batchDelete(ids);
        return BaseResponse.ok(null);
    }
}

