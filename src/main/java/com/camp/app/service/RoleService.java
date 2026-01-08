package com.camp.app.service;

import com.camp.app.api.PageResult;
import com.camp.app.model.RoleVo;

import java.util.List;

public interface RoleService {
    PageResult<RoleVo> list(long page, long size);
    RoleVo detail(Long id);
    void create(RoleVo vo);
    void update(Long id, RoleVo vo);
    void delete(Long id);
    void assignPermissions(Long id, List<Long> permissionIds);
}

