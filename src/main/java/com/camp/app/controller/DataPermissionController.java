package com.camp.app.controller;

import com.camp.app.api.BaseResponse;
import com.camp.app.api.PageResult;
import com.camp.app.model.DataQueryPermissionVo;
import com.camp.app.service.DataPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/data-permissions")
public class DataPermissionController {

    private final DataPermissionService dataPermissionService;

    @GetMapping
    public BaseResponse<PageResult<DataQueryPermissionVo>> list(@RequestParam(defaultValue = "1") long page,
                                                              @RequestParam(defaultValue = "20") long size,
                                                              @RequestParam(required = false) Long userId,
                                                              @RequestParam(required = false) Long roleId,
                                                              @RequestParam(required = false) String dataType) {
        return BaseResponse.ok(dataPermissionService.list(page, size, userId, roleId, dataType));
    }

    @GetMapping("/{id}")
    public BaseResponse<DataQueryPermissionVo> detail(@PathVariable Long id) {
        return BaseResponse.ok(dataPermissionService.detail(id));
    }

    @PostMapping
    public BaseResponse<Void> create(@RequestBody DataQueryPermissionVo permission) {
        dataPermissionService.create(permission);
        return BaseResponse.ok(null);
    }

    @PutMapping("/{id}")
    public BaseResponse<Void> update(@PathVariable Long id, @RequestBody DataQueryPermissionVo permission) {
        dataPermissionService.update(id, permission);
        return BaseResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable Long id) {
        dataPermissionService.delete(id);
        return BaseResponse.ok(null);
    }
}

