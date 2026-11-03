package com.camp.app.controller;

import com.camp.app.api.BaseResponse;
import com.camp.app.model.Phase2ResetPasswordRequest;
import com.camp.app.service.Phase2PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 短信通道关闭时的最小找回。打开 camp.sms.enabled 后仍走原 PasswordResetController。
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "camp.sms.enabled", havingValue = "false", matchIfMissing = true)
public class Phase2PasswordResetController {
    private final Phase2PasswordResetService phase2PasswordResetService;

    @PostMapping("/reset-password")
    public BaseResponse<Map<String, Boolean>> reset(@RequestBody Phase2ResetPasswordRequest request) {
        phase2PasswordResetService.reset(request);
        return BaseResponse.ok(Map.of("success", true));
    }
}
