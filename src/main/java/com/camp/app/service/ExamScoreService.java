package com.camp.app.service;

import com.camp.app.api.PageResult;
import com.camp.app.model.ExamScoreVo;

public interface ExamScoreService {
    PageResult<ExamScoreVo> list(long page, long size, Long studentId, Long courseId, Long examScheduleId, String semester, String academicYear, String examType);
    ExamScoreVo detail(Long id);
    void create(ExamScoreVo vo);
    void update(Long id, ExamScoreVo vo);
    void delete(Long id);
}

