package com.camp.app.controller;

import com.camp.app.api.BaseResponse;
import com.camp.app.api.PageResult;
import com.camp.app.model.VisitorRegistrationVo;
import com.camp.app.service.VisitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/visitors")
public class VisitorController {

    private final VisitorService visitorService;

    @GetMapping
    public BaseResponse<PageResult<VisitorRegistrationVo>> list(@RequestParam(defaultValue = "1") long page,
                                                              @RequestParam(defaultValue = "20") long size,
                                                              @RequestParam(required = false) String visitDate,
                                                              @RequestParam(required = false) Integer status,
                                                              @RequestParam(required = false) Long visitTargetUserId,
                                                              @RequestParam(required = false) String keyword) {
        return BaseResponse.ok(visitorService.list(page, size, visitDate, status, visitTargetUserId, keyword));
    }

    @GetMapping("/{id}")
    public BaseResponse<VisitorRegistrationVo> detail(@PathVariable Long id) {
        return BaseResponse.ok(visitorService.detail(id));
    }

    @PostMapping
    public BaseResponse<Void> create(@RequestBody VisitorRegistrationVo registration) {
        visitorService.create(registration);
        return BaseResponse.ok(null);
    }

    @PutMapping("/{id}")
    public BaseResponse<Void> update(@PathVariable Long id, @RequestBody VisitorRegistrationVo registration) {
        visitorService.update(id, registration);
        return BaseResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable Long id) {
        visitorService.delete(id);
        return BaseResponse.ok(null);
    }

    @PutMapping("/{id}/approve")
    public BaseResponse<Void> approve(@PathVariable Long id, @RequestParam Integer status, @RequestParam(required = false) String remark) {
        visitorService.approve(id, status, remark);
        return BaseResponse.ok(null);
    }

    @PutMapping("/{id}/reject")
    public BaseResponse<Void> reject(@PathVariable Long id, @RequestParam(required = false) String remark) {
        visitorService.reject(id, remark);
        return BaseResponse.ok(null);
    }

    @PutMapping("/{id}/entry")
    public BaseResponse<Void> entry(@PathVariable Long id, @RequestParam String entryTime) {
        visitorService.entry(id, entryTime);
        return BaseResponse.ok(null);
    }

    @PutMapping("/{id}/exit")
    public BaseResponse<Void> exit(@PathVariable Long id, @RequestParam String exitTime) {
        visitorService.exit(id, exitTime);
        return BaseResponse.ok(null);
    }
}

