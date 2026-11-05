package com.camp.app.controller;

import com.camp.app.api.BaseResponse;
import com.camp.app.api.PageResult;
import com.camp.app.api.Paging;
import com.camp.app.model.OaVisitorVo;
import com.camp.app.model.VisitorActionRequest;
import com.camp.app.service.OaVisitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/visitors")
@RequiredArgsConstructor
public class OaVisitorController {
    private final OaVisitorService oaVisitorService;

    @GetMapping
    public BaseResponse<PageResult<OaVisitorVo>> list(@RequestParam(defaultValue = "1") long page,
                                                      @RequestParam(name = "pageSize", required = false) Long pageSize,
                                                      @RequestParam(defaultValue = "20") long size,
                                                      @RequestParam(required = false) String status,
                                                      @RequestParam(required = false) Long hostUserId,
                                                      @RequestParam(required = false) String keyword) {
        return BaseResponse.ok(oaVisitorService.list(Paging.page(page), Paging.size(pageSize, size), status, hostUserId, keyword));
    }

    @GetMapping("/{id}")
    public BaseResponse<OaVisitorVo> detail(@PathVariable Long id) {
        return BaseResponse.ok(oaVisitorService.get(id));
    }

    @PostMapping
    public BaseResponse<OaVisitorVo> create(@RequestBody OaVisitorVo vo) {
        return BaseResponse.ok(oaVisitorService.create(vo));
    }

    @PutMapping("/{id}")
    public BaseResponse<OaVisitorVo> update(@PathVariable Long id, @RequestBody OaVisitorVo vo) {
        return BaseResponse.ok(oaVisitorService.update(id, vo));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable Long id) {
        oaVisitorService.delete(id);
        return BaseResponse.ok(null);
    }

    @PostMapping("/{id}/approve")
    public BaseResponse<OaVisitorVo> approve(@PathVariable Long id,
                                             @RequestBody(required = false) VisitorActionRequest body) {
        return BaseResponse.ok(oaVisitorService.approve(id, remark(body)));
    }

    @PostMapping("/{id}/reject")
    public BaseResponse<OaVisitorVo> reject(@PathVariable Long id,
                                            @RequestBody(required = false) VisitorActionRequest body) {
        return BaseResponse.ok(oaVisitorService.reject(id, remark(body)));
    }

    @PostMapping("/{id}/check-in")
    public BaseResponse<OaVisitorVo> checkIn(@PathVariable Long id,
                                             @RequestBody(required = false) VisitorActionRequest body) {
        return BaseResponse.ok(oaVisitorService.checkIn(id, remark(body)));
    }

    @PostMapping("/{id}/check-out")
    public BaseResponse<OaVisitorVo> checkOut(@PathVariable Long id,
                                              @RequestBody(required = false) VisitorActionRequest body) {
        return BaseResponse.ok(oaVisitorService.checkOut(id, remark(body)));
    }

    private String remark(VisitorActionRequest body) {
        return body == null ? null : body.getRemark();
    }
}
