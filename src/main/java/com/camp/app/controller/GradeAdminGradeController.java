package com.camp.app.controller;

import com.camp.app.api.BaseResponse;
import com.camp.app.api.PageResult;
import com.camp.app.model.GradeAdminGradeVo;
import com.camp.app.service.GradeAdminGradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/grade-admins/{adminId}/grades")
public class GradeAdminGradeController {

    private final GradeAdminGradeService gradeAdminGradeService;

    @GetMapping
    public BaseResponse<PageResult<GradeAdminGradeVo>> list(@PathVariable Long adminId,
                                                          @RequestParam(defaultValue = "1") long page,
                                                          @RequestParam(defaultValue = "20") long size) {
        return BaseResponse.ok(gradeAdminGradeService.list(adminId, page, size));
    }

    @PostMapping
    public BaseResponse<Void> create(@PathVariable Long adminId, @RequestBody GradeAdminGradeVo gag) {
        gradeAdminGradeService.create(adminId, gag);
        return BaseResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable Long adminId, @PathVariable Long id) {
        gradeAdminGradeService.delete(adminId, id);
        return BaseResponse.ok(null);
    }
}

