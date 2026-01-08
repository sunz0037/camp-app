package com.camp.app.service;

import com.camp.app.api.PageResult;
import com.camp.app.model.CourseVo;

import java.util.List;

public interface CourseService {
    PageResult<CourseVo> list(long page, long size, String semester, String academicYear, Long teacherId, String keyword);
    CourseVo detail(Long id);
    void create(CourseVo vo);
    void update(Long id, CourseVo vo);
    void delete(Long id);
    
    /**
     * 获取所有启用的科目
     */
    List<CourseVo> getAllEnabledCourses();
}

