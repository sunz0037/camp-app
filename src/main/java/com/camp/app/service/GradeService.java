package com.camp.app.service;

import com.camp.app.api.PageResult;
import com.camp.app.model.GradeVo;

import java.util.List;

/**
 * 年级服务接口
 */
public interface GradeService {
    
    /**
     * 分页查询年级列表
     */
    PageResult<GradeVo> list(long page, long size, String gradeType, Integer status, String keyword);
    
    /**
     * 查询年级详情
     */
    GradeVo detail(Long id);
    
    /**
     * 创建年级
     */
    void create(GradeVo grade);
    
    /**
     * 更新年级
     */
    void update(Long id, GradeVo grade);
    
    /**
     * 删除年级
     */
    void delete(Long id);
    
    /**
     * 查询所有启用的年级（不分页）
     */
    List<GradeVo> listAllEnabled();
    
    /**
     * 根据年级类型查询
     */
    List<GradeVo> listByType(String gradeType);
}

