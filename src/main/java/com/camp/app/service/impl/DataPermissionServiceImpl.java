package com.camp.app.service.impl;

import com.camp.app.api.PageResult;
import com.camp.app.model.DataQueryPermissionVo;
import com.camp.app.service.DataPermissionService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class DataPermissionServiceImpl implements DataPermissionService {
    @Override
    public PageResult<DataQueryPermissionVo> list(long page, long size, Long userId, Long roleId, String dataType) {
        return PageResult.<DataQueryPermissionVo>builder().page(page).size(size).total(0).records(new ArrayList<>()).build();
    }

    @Override
    public DataQueryPermissionVo detail(Long id) {
        return null;
    }

    @Override
    public void create(DataQueryPermissionVo vo) {
        // TODO implement
    }

    @Override
    public void update(Long id, DataQueryPermissionVo vo) {
        // TODO implement
    }

    @Override
    public void delete(Long id) {
        // TODO implement
    }
}

