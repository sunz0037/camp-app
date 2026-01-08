package com.camp.app.service;

import com.camp.app.model.ClassCourseVo;
import java.util.List;

/**
 * 班级授课科目关联服务接口
 */
public interface ClassCourseService {
    
    /**
     * 查询班级的所有授课科目
     */
    List<ClassCourseVo> listByClassId(Long classId);
    
    /**
     * 查询班级的启用授课科目
     */
    List<ClassCourseVo> listEnabledByClassId(Long classId);
    
    /**
     * 根据ID查询详情
     */
    ClassCourseVo detail(Long id);
    
    /**
     * 添加班级授课科目
     */
    void create(ClassCourseVo classCourseVo);
    
    /**
     * 批量添加班级授课科目
     */
    void batchCreate(Long classId, List<Long> courseIds);
    
    /**
     * 更新班级授课科目
     */
    void update(Long id, ClassCourseVo classCourseVo);
    
    /**
     * 删除班级授课科目
     */
    void delete(Long id);
    
    /**
     * 批量删除班级授课科目
     */
    void batchDelete(List<Long> ids);
}

