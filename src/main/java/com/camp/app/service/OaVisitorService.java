package com.camp.app.service;

import com.camp.app.api.PageResult;
import com.camp.app.model.OaVisitorVo;

public interface OaVisitorService {
    PageResult<OaVisitorVo> list(long page, long size, String status, Long hostUserId, String keyword);

    OaVisitorVo get(Long id);

    OaVisitorVo create(OaVisitorVo vo);

    OaVisitorVo update(Long id, OaVisitorVo vo);

    void delete(Long id);

    OaVisitorVo approve(Long id, String remark);

    OaVisitorVo reject(Long id, String remark);

    OaVisitorVo checkIn(Long id, String remark);

    OaVisitorVo checkOut(Long id, String remark);
}
