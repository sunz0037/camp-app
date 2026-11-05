package com.camp.app.controller;

import com.camp.app.api.BaseResponse;
import com.camp.app.api.PageResult;
import com.camp.app.api.Paging;
import com.camp.app.model.CourseVo;
import com.camp.app.model.OptionItem;
import com.camp.app.security.Phase1Permissions;
import com.camp.app.security.RequirePermission;
import com.camp.app.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/courses")
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    public BaseResponse<PageResult<CourseVo>> list(@RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(name = "pageSize", required = false) Long pageSize,
                                                 @RequestParam(defaultValue = "20") long size,
                                                 @RequestParam(required = false) String semester,
                                                 @RequestParam(required = false) String academicYear,
                                                 @RequestParam(required = false) Long teacherId,
                                                 @RequestParam(required = false) String keyword) {
        return BaseResponse.ok(courseService.list(Paging.page(page), Paging.size(pageSize, size), semester, academicYear, teacherId, keyword));
    }

    @GetMapping("/options")
    public BaseResponse<List<OptionItem>> options() {
        return BaseResponse.ok(courseService.getAllEnabledCourses().stream()
                .map(course -> new OptionItem(course.getId(), course.getCourseName(), null))
                .toList());
    }

    @GetMapping("/{id}")
    public BaseResponse<CourseVo> detail(@PathVariable Long id) {
        return BaseResponse.ok(courseService.detail(id));
    }

    @PostMapping
    @RequirePermission(Phase1Permissions.COURSE_WRITE)
    public BaseResponse<Void> create(@RequestBody CourseVo course) {
        courseService.create(course);
        return BaseResponse.ok(null);
    }

    @PutMapping("/{id}")
    @RequirePermission(Phase1Permissions.COURSE_WRITE)
    public BaseResponse<Void> update(@PathVariable Long id, @RequestBody CourseVo course) {
        courseService.update(id, course);
        return BaseResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    @RequirePermission(Phase1Permissions.COURSE_WRITE)
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

