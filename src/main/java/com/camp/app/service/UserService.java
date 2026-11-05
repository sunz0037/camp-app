package com.camp.app.service;

import com.camp.app.api.PageResult;
import com.camp.app.model.UserVo;

/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 分页查询用户列表
     */
    PageResult<UserVo> list(long page, long size, Integer userType, Integer status, String keyword);
    
    /**
     * 查询用户详情
     */
    UserVo detail(Long id);
    
    /**
     * 创建用户
     */
    UserVo create(UserVo userVo);
    
    /**
     * 更新用户信息。password 字段会被忽略，初始密码只在创建时写入。
     */
    UserVo update(Long id, UserVo userVo);
    
    /**
     * 删除用户
     */
    void delete(Long id);
    
    /**
     * 启用/禁用用户
     */
    void updateStatus(Long id, Integer status);
}
