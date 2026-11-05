package com.camp.app.controller;

import com.camp.app.api.BaseResponse;
import com.camp.app.model.DashboardOverviewVo;
import com.camp.app.security.AuthContext;
import com.camp.app.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping("/overview")
    public BaseResponse<DashboardOverviewVo> overview() {
        return BaseResponse.ok(dashboardService.overview(AuthContext.userId()));
    }
}
