package com.camp.app.controller;

import com.camp.app.api.BaseResponse;
import com.camp.app.api.PageResult;
import com.camp.app.model.StudentVo;
import com.camp.app.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;

    @GetMapping
    public BaseResponse<PageResult<StudentVo>> listStudents(@RequestParam(defaultValue = "1") long page,
                                                          @RequestParam(defaultValue = "20") long size,
                                                          @RequestParam(required = false) String grade,
                                                          @RequestParam(required = false) String className,
                                                          @RequestParam(required = false) String major,
                                                          @RequestParam(required = false) String keyword) {
        return BaseResponse.ok(studentService.list(page, size, grade, className, major, keyword));
    }

    @GetMapping("/{id}")
    public BaseResponse<StudentVo> getStudent(@PathVariable Long id) {
        return BaseResponse.ok(studentService.get(id));
    }

    @PostMapping
    public BaseResponse<Void> createStudent(@RequestBody StudentVo student) {
        studentService.create(student);
        return BaseResponse.ok(null);
    }

    @PutMapping("/{id}")
    public BaseResponse<Void> updateStudent(@PathVariable Long id, @RequestBody StudentVo student) {
        studentService.update(id, student);
        return BaseResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> deleteStudent(@PathVariable Long id) {
        studentService.delete(id);
        return BaseResponse.ok(null);
    }
}

