package com.camp.app.controller;

import com.camp.app.api.BaseResponse;
import com.camp.app.api.PageResult;
import com.camp.app.model.AnnouncementVo;
import com.camp.app.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping
    public BaseResponse<PageResult<AnnouncementVo>> list(@RequestParam(defaultValue = "1") long page,
                                                       @RequestParam(defaultValue = "20") long size,
                                                       @RequestParam(required = false) String category,
                                                       @RequestParam(required = false) Integer status,
                                                       @RequestParam(required = false) Integer isTop,
                                                       @RequestParam(required = false) Integer isUrgent,
                                                       @RequestParam(required = false) String keyword) {
        return BaseResponse.ok(announcementService.list(page, size, category, status, isTop, isUrgent, keyword));
    }

    @GetMapping("/{id}")
    public BaseResponse<AnnouncementVo> detail(@PathVariable Long id) {
        return BaseResponse.ok(announcementService.detail(id));
    }

    @PostMapping
    public BaseResponse<Void> create(@RequestBody AnnouncementVo announcement) {
        announcementService.create(announcement);
        return BaseResponse.ok(null);
    }

    @PutMapping("/{id}")
    public BaseResponse<Void> update(@PathVariable Long id, @RequestBody AnnouncementVo announcement) {
        announcementService.update(id, announcement);
        return BaseResponse.ok(null);
    }

    @PutMapping("/{id}/top")
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

