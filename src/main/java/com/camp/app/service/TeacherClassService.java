package com.camp.app.service;

import com.camp.app.api.PageResult;
import com.camp.app.model.TeacherClassVo;

public interface TeacherClassService {
    PageResult<TeacherClassVo> list(Long teacherId, long page, long size);
    void create(Long teacherId, TeacherClassVo vo);
    void delete(Long teacherId, Long id);
}

