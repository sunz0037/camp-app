package com.camp.app.controller;

import com.camp.app.api.BaseResponse;
import com.camp.app.api.PageResult;
import com.camp.app.model.PermissionGrantRequest;
import com.camp.app.model.RoleVo;
import com.camp.app.security.Phase1Permissions;
import com.camp.app.security.RequirePermission;
import com.camp.app.service.RoleService;
import com.camp.app.service.UserRoleAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleService roleService;
    private final UserRoleAdminService userRoleAdminService;

    @GetMapping
    public BaseResponse<PageResult<RoleVo>> list(@RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "20") long size) {
        return BaseResponse.ok(roleService.list(page, size));
    }

    @GetMapping("/{id}")
    public BaseResponse<RoleVo> detail(@PathVariable Long id) {
        return BaseResponse.ok(roleService.detail(id));
    }

    @PostMapping
    public BaseResponse<Void> create(@RequestBody RoleVo role) {
        roleService.create(role);
        return BaseResponse.ok(null);
    }

    @PutMapping("/{id}")
    public BaseResponse<Void> update(@PathVariable Long id, @RequestBody RoleVo role) {
        roleService.update(id, role);
        return BaseResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return BaseResponse.ok(null);
    }

    @PutMapping("/{id}/permissions")
    @RequirePermission(Phase1Permissions.ROLE_WRITE)
    public BaseResponse<Void> replacePermissions(@PathVariable Long id, @RequestBody PermissionGrantRequest request) {
        userRoleAdminService.replacePermissionCodes(id, request.getPermissionCodes());
        return BaseResponse.ok(null);
    }

    @PostMapping("/{id}/permissions")
    @RequirePermission(Phase1Permissions.ROLE_WRITE)
    public BaseResponse<Void> assignPermissions(@PathVariable Long id, @RequestBody List<Long> permissionIds) {
        roleService.assignPermissions(id, permissionIds);
        return BaseResponse.ok(null);
    }
}

