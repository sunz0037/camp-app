package com.camp.app.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class DashboardOverviewVo {
    private String role;
    private Map<String, Object> metrics;
    private List<TimetableItemVo> todayTimetable;
    private List<AnnouncementVo> latestAnnouncements;
    private List<String> shortcuts;
}
