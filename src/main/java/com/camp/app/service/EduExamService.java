package com.camp.app.service;

import com.camp.app.api.PageResult;
import com.camp.app.model.EduExamVo;

public interface EduExamService {
    PageResult<EduExamVo> list(long page, long size, Long gradeId, Long classId, String examType,
                               Boolean isPublic, String status, String keyword);

    EduExamVo get(Long id);

    boolean visibleToCurrentUser(Long id);

    EduExamVo create(EduExamVo vo);

    EduExamVo update(Long id, EduExamVo vo);

    void delete(Long id);
}
