package com.camp.app.service;

import com.camp.app.api.PageResult;
import com.camp.app.model.ClassVo;
import com.camp.app.model.OptionItem;

import java.util.List;

/**
 * 班级服务接口
 */
public interface ClassService {
    
    /**
     * 分页查询班级列表
     */
    PageResult<ClassVo> list(long page, long size, Long gradeId, String gradeType, Integer status, String keyword);
    
    /**
     * 查询班级详情
     */
    ClassVo detail(Long id);
    
    /**
     * 创建班级
     */
    void create(ClassVo classVo);
    
    /**
     * 更新班级
     */
    void update(Long id, ClassVo classVo);
    
    /**
     * 删除班级
     */
    void delete(Long id);
    
    /**
     * 根据年级ID查询班级列表
     */
    List<ClassVo> listByGradeId(Long gradeId);
    
    /**
     * 根据年级ID查询启用的班级列表
     */
    List<ClassVo> listEnabledByGradeId(Long gradeId);

    List<OptionItem> options(Long gradeId);
}

