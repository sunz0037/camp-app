package com.camp.app.service;

import com.camp.app.api.PageResult;
import com.camp.app.model.ExamScheduleVo;

/**
 * 考试安排服务接口
 */
public interface ExamScheduleService {
    
    /**
     * 分页查询考试安排
     */
    PageResult<ExamScheduleVo> list(Integer page, Integer size, String targetGrade, 
                                    String examType, Boolean isPublic, Integer status, 
                                    String keyword);
    
    /**
     * 根据ID查询考试安排
     */
    ExamScheduleVo getById(Long id);
    
    /**
     * 创建考试安排
     */
    ExamScheduleVo create(ExamScheduleVo vo, Long createdBy);
    
    /**
     * 更新考试安排
     */
    ExamScheduleVo update(Long id, ExamScheduleVo vo);
    
    /**
     * 删除考试安排
     */
    void delete(Long id);
    
    /**
     * 取消考试安排（将状态设为0）
     */
    ExamScheduleVo cancel(Long id);
}

