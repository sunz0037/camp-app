package com.camp.app.service;

import com.camp.app.api.PageResult;
import com.camp.app.model.EduExamScoreVo;

public interface EduExamScoreService {
    PageResult<EduExamScoreVo> list(long page, long size, Long examId, Long studentId, Long classId, String keyword);

    EduExamScoreVo get(Long id);

    EduExamScoreVo create(EduExamScoreVo vo);

    EduExamScoreVo update(Long id, EduExamScoreVo vo);

    void delete(Long id);
}
