package com.camp.app.service;

import com.camp.app.api.PageResult;
import com.camp.app.model.GradeAdminGradeVo;

public interface GradeAdminGradeService {
    PageResult<GradeAdminGradeVo> list(Long adminId, long page, long size);
    void create(Long adminId, GradeAdminGradeVo vo);
    void delete(Long adminId, Long id);
}

