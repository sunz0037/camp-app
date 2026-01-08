package com.camp.app.service.impl;

import com.camp.app.api.PageResult;
import com.camp.app.model.TeacherClassVo;
import com.camp.app.service.TeacherClassService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class TeacherClassServiceImpl implements TeacherClassService {
    @Override
    public PageResult<TeacherClassVo> list(Long teacherId, long page, long size) {
        return PageResult.<TeacherClassVo>builder().page(page).size(size).total(0).records(new ArrayList<>()).build();
    }

    @Override
    public void create(Long teacherId, TeacherClassVo vo) {
        // TODO implement
    }

    @Override
    public void delete(Long teacherId, Long id) {
        // TODO implement
    }
}

