package com.camp.app.service;

import com.camp.app.model.PermissionVo;

import java.util.List;

public interface PermissionService {
    List<PermissionVo> listAll();

    List<PermissionVo> tree();
}

