package com.camp.app.controller;

import com.camp.app.api.BaseResponse;
import com.camp.app.model.PermissionVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
@lombok.RequiredArgsConstructor
public class PermissionController {

    private final com.camp.app.service.PermissionService permissionService;

    @GetMapping
    public BaseResponse<List<PermissionVo>> listAll() {
        return BaseResponse.ok(permissionService.listAll());
    }
}

