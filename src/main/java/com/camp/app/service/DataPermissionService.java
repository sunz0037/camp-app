package com.camp.app.service;

import com.camp.app.api.PageResult;
import com.camp.app.model.DataQueryPermissionVo;

public interface DataPermissionService {
    PageResult<DataQueryPermissionVo> list(long page, long size, Long userId, Long roleId, String dataType);
    DataQueryPermissionVo detail(Long id);
    void create(DataQueryPermissionVo vo);
    void update(Long id, DataQueryPermissionVo vo);
    void delete(Long id);
}

