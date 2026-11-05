package com.camp.app.service;

import com.camp.app.api.PageResult;
import com.camp.app.model.ExamScheduleVo;

/**
 * 考试安排服务接口
 */
public interface ExamScheduleService {
    String UNSUPPORTED_FILTER = "edu_exams 不支持按年级名称、考试类型、公开标记或状态筛选";

    /**
     * 分页查询考试安排
     */
    PageResult<ExamScheduleVo> list(Integer page, Integer size, String targetGrade,
                                    String examType, Boolean isPublic, Integer status,
                                    String keyword);

    /**
     * 与上面的列表同一张表、同一数据范围。
     * gradeName、classId、courseId、academicYear、semester 只在非空时收窄。
     * targetGrade、examType、isPublic、status 仍然拒绝，因为表上没有这些列。
     */
    PageResult<ExamScheduleVo> list(Integer page, Integer size, String targetGrade,
                                    String examType, Boolean isPublic, Integer status,
                                    String keyword, String gradeName, Long classId, Long courseId,
                                    String academicYear, Integer semester);
    
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

