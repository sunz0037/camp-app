package com.camp.app.controller;

import com.camp.app.api.BaseResponse;
import com.camp.app.api.PageResult;
import com.camp.app.model.UserVo;
import com.camp.app.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @GetMapping
    public BaseResponse<PageResult<UserVo>> listUsers(@RequestParam(defaultValue = "1") long page,
                                                      @RequestParam(defaultValue = "20") long size,
                                                      @RequestParam(required = false) Integer userType,
                                                      @RequestParam(required = false) Integer status,
                                                      @RequestParam(required = false) String keyword) {
        return BaseResponse.ok(userService.list(page, size, userType, status, keyword));
    }

    @GetMapping("/{id}")
    public BaseResponse<UserVo> getUser(@PathVariable Long id) {
        return BaseResponse.ok(userService.detail(id));
    }

    @PostMapping
    public BaseResponse<Void> createUser(@RequestBody UserVo user) {
        userService.create(user);
        return BaseResponse.ok(null);
    }

    @PutMapping("/{id}")
    public BaseResponse<Void> updateUser(@PathVariable Long id, @RequestBody UserVo user) {
        userService.update(id, user);
        return BaseResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return BaseResponse.ok(null);
    }

    @PutMapping("/{id}/status")
    public BaseResponse<Void> updateUserStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return BaseResponse.ok(null);
    }
}
