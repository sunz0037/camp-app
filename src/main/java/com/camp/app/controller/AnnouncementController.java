package com.camp.app.controller;

import com.camp.app.api.BaseResponse;
import com.camp.app.api.PageResult;
import com.camp.app.api.Paging;
import com.camp.app.model.AnnouncementVo;
import com.camp.app.security.Phase1Permissions;
import com.camp.app.security.RequirePermission;
import com.camp.app.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping
    public BaseResponse<PageResult<AnnouncementVo>> list(@RequestParam(defaultValue = "1") long page,
                                                       @RequestParam(name = "pageSize", required = false) Long pageSize,
                                                       @RequestParam(defaultValue = "20") long size,
                                                       @RequestParam(required = false) String category,
                                                       @RequestParam(required = false) Integer status,
                                                       @RequestParam(required = false) Integer isTop,
                                                       @RequestParam(required = false) Integer isUrgent,
                                                       @RequestParam(required = false) String keyword) {
        return BaseResponse.ok(announcementService.list(Paging.page(page), Paging.size(pageSize, size), category, status, isTop, isUrgent, keyword));
    }

    @GetMapping("/unread-count")
    public BaseResponse<java.util.Map<String, Long>> unreadCount() {
        return BaseResponse.ok(java.util.Map.of("count", announcementService.unreadCount()));
    }

    @GetMapping("/{id}")
    public BaseResponse<AnnouncementVo> detail(@PathVariable Long id) {
        return BaseResponse.ok(announcementService.detail(id));
    }

    @PostMapping("/{id}/read")
    public BaseResponse<java.util.Map<String, Boolean>> read(@PathVariable Long id) {
        announcementService.markRead(id);
        return BaseResponse.ok(java.util.Map.of("isRead", true));
    }

    @PostMapping
    @RequirePermission(Phase1Permissions.ANNOUNCEMENT_PUBLISH)
    public BaseResponse<AnnouncementVo> create(@RequestBody AnnouncementVo announcement) {
        return BaseResponse.ok(announcementService.create(announcement));
    }

    @PutMapping("/{id}")
    public BaseResponse<AnnouncementVo> update(@PathVariable Long id, @RequestBody AnnouncementVo announcement) {
        return BaseResponse.ok(announcementService.update(id, announcement));
    }

    @PutMapping("/{id}/top")
    @RequirePermission(Phase1Permissions.ANNOUNCEMENT_TOP)
    public BaseResponse<Void> top(@PathVariable Long id, @RequestParam Integer isTop) {
        announcementService.top(id, isTop);
        return BaseResponse.ok(null);
    }

    @PutMapping("/{id}/withdraw")
    public BaseResponse<Void> withdraw(@PathVariable Long id) {
        announcementService.withdraw(id);
        return BaseResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable Long id) {
        announcementService.delete(id);
        return BaseResponse.ok(null);
    }
}

