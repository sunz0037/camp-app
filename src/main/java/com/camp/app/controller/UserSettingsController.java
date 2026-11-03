package com.camp.app.controller;

import com.camp.app.api.BaseResponse;
import com.camp.app.model.UserSettingsVo;
import com.camp.app.service.UserSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/settings")
@RequiredArgsConstructor
public class UserSettingsController {
    private final UserSettingsService userSettingsService;

    @GetMapping
    public BaseResponse<UserSettingsVo> get() {
        return BaseResponse.ok(userSettingsService.getMine());
    }

    @PutMapping
    public BaseResponse<UserSettingsVo> update(@RequestBody UserSettingsVo vo) {
        return BaseResponse.ok(userSettingsService.updateMine(vo));
    }
}
