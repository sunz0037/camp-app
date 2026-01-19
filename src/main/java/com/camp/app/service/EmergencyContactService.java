package com.camp.app.service;

import com.camp.app.model.EmergencyContactVo;

import java.util.List;

/**
 * 应急联系人服务接口
 */
public interface EmergencyContactService {
    
    /**
     * 根据学生ID查询所有应急联系人
     * 
     * @param studentId 学生ID
     * @return 应急联系人列表
     */
    List<EmergencyContactVo> listByStudentId(Long studentId);
    
    /**
     * 查询应急联系人详情
     * 
     * @param id 应急联系人ID
     * @return 应急联系人详情
     */
    EmergencyContactVo detail(Long id);
    
    /**
     * 创建应急联系人
     * 
     * @param vo 应急联系人信息
     * @return 创建的应急联系人
     */
    EmergencyContactVo create(EmergencyContactVo vo);
    
    /**
     * 更新应急联系人
     * 
     * @param id 应急联系人ID
     * @param vo 应急联系人信息
     */
    void update(Long id, EmergencyContactVo vo);
    
    /**
     * 删除应急联系人
     * 
     * @param id 应急联系人ID
     */
    void delete(Long id);
    
    /**
     * 设置主要联系人
     * 
     * @param studentId 学生ID
     * @param contactId 应急联系人ID
     */
    void setPrimary(Long studentId, Long contactId);
}
