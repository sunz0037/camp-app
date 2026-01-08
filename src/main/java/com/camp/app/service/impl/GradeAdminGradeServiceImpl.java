package com.camp.app.service.impl;

import com.camp.app.api.PageResult;
import com.camp.app.model.GradeAdminGradeVo;
import com.camp.app.service.GradeAdminGradeService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class GradeAdminGradeServiceImpl implements GradeAdminGradeService {
    @Override
    public PageResult<GradeAdminGradeVo> list(Long adminId, long page, long size) {
        return PageResult.<GradeAdminGradeVo>builder().page(page).size(size).total(0).records(new ArrayList<>()).build();
    }

    @Override
    public void create(Long adminId, GradeAdminGradeVo vo) {
        // TODO implement
    }

    @Override
    public void delete(Long adminId, Long id) {
        // TODO implement
    }
}

