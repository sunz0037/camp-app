package com.camp.app.controller;

import com.camp.app.api.BaseResponse;
import com.camp.app.model.IdListRequest;
import com.camp.app.model.OptionItem;
import com.camp.app.security.Phase1Permissions;
import com.camp.app.security.RequirePermission;
import com.camp.app.service.AcademicGradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/academics/{userId}/grades")
@RequiredArgsConstructor
public class AcademicGradeController {
    private final AcademicGradeService academicGradeService;

    @GetMapping
    @RequirePermission(Phase1Permissions.USER_WRITE)
    public BaseResponse<List<OptionItem>> list(@PathVariable Long userId) {
        return BaseResponse.ok(academicGradeService.list(userId));
    }

    @PutMapping
    @RequirePermission(Phase1Permissions.ROLE_WRITE)
    public BaseResponse<Void> replace(@PathVariable Long userId, @RequestBody IdListRequest request) {
        academicGradeService.replace(userId, request.getGradeIds());
        return BaseResponse.ok(null);
    }
}
