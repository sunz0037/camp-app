package com.camp.app.service;

import com.camp.app.api.PageResult;
import com.camp.app.model.ExamScoreVo;

public interface ExamScoreService {
    PageResult<ExamScoreVo> list(long page, long size, Long studentId, Long courseId, Long examScheduleId,
                                 String semester, String academicYear, String examType,
                                 Long classId, String className, String keyword);
    ExamScoreVo detail(Long id);
    ExamScoreVo create(ExamScoreVo vo);
    ExamScoreVo update(Long id, ExamScoreVo vo);
    void delete(Long id);
}

