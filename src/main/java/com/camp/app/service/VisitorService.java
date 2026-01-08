package com.camp.app.service;

import com.camp.app.api.PageResult;
import com.camp.app.model.VisitorRegistrationVo;

public interface VisitorService {
    PageResult<VisitorRegistrationVo> list(long page, long size, String visitDate, Integer status, Long visitTargetUserId, String keyword);
    VisitorRegistrationVo detail(Long id);
    void create(VisitorRegistrationVo vo);
    void update(Long id, VisitorRegistrationVo vo);
    void delete(Long id);
    void approve(Long id, Integer status, String remark);
    void reject(Long id, String remark);
    void entry(Long id, String entryTime);
    void exit(Long id, String exitTime);
}

