package com.camp.app.service;

import com.camp.app.model.DashboardOverviewVo;

public interface DashboardService {
    DashboardOverviewVo overview(Long userId);
}
