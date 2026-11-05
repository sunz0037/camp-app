package com.camp.app.service;

import com.camp.app.api.PageResult;
import com.camp.app.model.TeacherClassVo;

import java.util.List;

public interface TeacherClassService {
    PageResult<TeacherClassVo> list(Long teacherId, long page, long size);
    void create(Long teacherId, TeacherClassVo vo);
    void delete(Long teacherId, Long id);
    void replace(Long teacherId, List<Long> classIds);
}

