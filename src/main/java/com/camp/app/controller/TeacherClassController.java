package com.camp.app.controller;

import com.camp.app.api.BaseResponse;
import com.camp.app.api.PageResult;
import com.camp.app.model.TeacherClassVo;
import com.camp.app.service.TeacherClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/teachers/{teacherId}/classes")
public class TeacherClassController {

    private final TeacherClassService teacherClassService;

    @GetMapping
    public BaseResponse<PageResult<TeacherClassVo>> list(@PathVariable Long teacherId,
                                                       @RequestParam(defaultValue = "1") long page,
                                                       @RequestParam(defaultValue = "20") long size) {
        return BaseResponse.ok(teacherClassService.list(teacherId, page, size));
    }

    @PostMapping
    public BaseResponse<Void> create(@PathVariable Long teacherId, @RequestBody TeacherClassVo tc) {
        teacherClassService.create(teacherId, tc);
        return BaseResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable Long teacherId, @PathVariable Long id) {
        teacherClassService.delete(teacherId, id);
        return BaseResponse.ok(null);
    }
}

