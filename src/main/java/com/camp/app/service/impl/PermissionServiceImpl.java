package com.camp.app.service.impl;

import com.camp.app.model.PermissionVo;
import com.camp.app.service.PermissionService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PermissionServiceImpl implements PermissionService {
    @Override
    public List<PermissionVo> listAll() {
        // TODO: 实现权限列表查询
        return new ArrayList<>();
    }
}

