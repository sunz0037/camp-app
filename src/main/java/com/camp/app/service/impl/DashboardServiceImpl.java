package com.camp.app.service.impl;

import com.camp.app.entity.User;
import com.camp.app.model.AnnouncementVo;
import com.camp.app.model.DashboardOverviewVo;
import com.camp.app.model.DataScope;
import com.camp.app.model.TimetableItemVo;
import com.camp.app.repository.StudentRepository;
import com.camp.app.repository.UserRepository;
import com.camp.app.security.AuthContext;
import com.camp.app.security.AuthPrincipal;
import com.camp.app.security.Phase1Roles;
import com.camp.app.service.AccessService;
import com.camp.app.service.AnnouncementService;
import com.camp.app.service.ClassCourseService;
import com.camp.app.service.DashboardService;
import com.camp.app.api.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    private final AccessService accessService;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final AnnouncementService announcementService;
    private final ClassCourseService classCourseService;

    @Override
    @Transactional(readOnly = true)
    public DashboardOverviewVo overview(Long userId) {
        if (userId == null) {
            throw BizException.unauthorized("未登录");
        }
        AuthPrincipal principal = AuthContext.get();
        boolean dev = principal != null && principal.devAuth();
        User user = userRepository.findById(userId).orElse(null);
        if (user == null && !dev) {
            throw BizException.notFound("用户不存在");
        }
        List<String> roles = dev ? principal.roles() : accessService.rolesOf(user);
        String highest = Phase1Roles.highest(roles);
        DataScope scope = accessService.scopeOf(userId);
        long studentCount = studentRepository.findAll().stream()
                .filter(student -> !Boolean.TRUE.equals(student.getIsDeleted()))
                .filter(student -> scope.isAll()
                        || ("SELF".equals(scope.getLevel()) && student.getId().equals(scope.getStudentId()))
                        || (scope.getClassIds() != null && scope.getClassIds().contains(student.getClassId())))
                .count();
        long unread = announcementService.unreadCount();
        int weekday = LocalDate.now().getDayOfWeek().getValue();
        List<TimetableItemVo> today = classCourseService.myTimetable(userId).stream()
                .filter(item -> item.getWeekday() != null && item.getWeekday() == weekday)
                .toList();
        List<AnnouncementVo> latest = announcementService.list(1, 5, null, 1, null, null, null).getRecords();
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("studentCount", studentCount);
        metrics.put("unreadAnnouncementCount", unread);
        metrics.put("todayLessonCount", today.size());
        return DashboardOverviewVo.builder()
                .role(highest)
                .metrics(metrics)
                .todayTimetable(today)
                .latestAnnouncements(latest)
                .shortcuts(dev ? principal.permissions() : accessService.permissionsOf(user))
                .build();
    }
}
