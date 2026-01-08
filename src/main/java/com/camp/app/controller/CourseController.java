package com.camp.app.controller;

import com.camp.app.api.BaseResponse;
import com.camp.app.api.PageResult;
import com.camp.app.model.CourseVo;
import com.camp.app.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    public BaseResponse<PageResult<CourseVo>> list(@RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "20") long size,
                                                 @RequestParam(required = false) String semester,
                                                 @RequestParam(required = false) String academicYear,
                                                 @RequestParam(required = false) Long teacherId,
                                                 @RequestParam(required = false) String keyword) {
        return BaseResponse.ok(courseService.list(page, size, semester, academicYear, teacherId, keyword));
    }

    @GetMapping("/{id}")
    public BaseResponse<CourseVo> detail(@PathVariable Long id) {
        return BaseResponse.ok(courseService.detail(id));
    }

    @PostMapping
    public BaseResponse<Void> create(@RequestBody CourseVo course) {
        courseService.create(course);
        return BaseResponse.ok(null);
    }

    @PutMapping("/{id}")
    public BaseResponse<Void> update(@PathVariable Long id, @RequestBody CourseVo course) {
        courseService.update(id, course);
        return BaseResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable Long id) {
        courseService.delete(id);
        return BaseResponse.ok(null);
    }
    
    /**
     * 获取所有启用的科目
     */
    @GetMapping("/enabled")
    public BaseResponse<List<CourseVo>> getAllEnabledCourses() {
        return BaseResponse.ok(courseService.getAllEnabledCourses());
    }
}

