package com.camp.app.service.impl;

import com.camp.app.api.PageResult;
import com.camp.app.entity.Role;
import com.camp.app.model.RoleVo;
import com.camp.app.repository.RoleRepository;
import com.camp.app.service.RoleService;
import com.camp.app.util.BeanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {
    private final RoleRepository roleRepository;

    @Override
    public PageResult<RoleVo> list(long page, long size) {
        Pageable pageable = PageRequest.of((int) (page - 1), (int) size);
        Page<Role> rolePage = roleRepository.findAll(pageable);
        List<RoleVo> voList = BeanMapper.mapList(rolePage.getContent(), RoleVo.class);
        
        return PageResult.<RoleVo>builder()
                .page(page)
                .size(size)
                .total(rolePage.getTotalElements())
                .records(voList)
                .build();
    }

    @Override
    public RoleVo detail(Long id) {
        return roleRepository.findById(id)
                .map(role -> BeanMapper.map(role, RoleVo.class))
                .orElse(null);
    }

    @Override
    @Transactional
    public void create(RoleVo vo) {
        Role role = BeanMapper.map(vo, Role.class);
        roleRepository.save(role);
    }

    @Override
    @Transactional
    public void update(Long id, RoleVo vo) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found: " + id));
        BeanMapper.copyProperties(vo, role);
        role.setId(id);
        roleRepository.save(role);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        roleRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void assignPermissions(Long id, List<Long> permissionIds) {
        // TODO: 实现角色权限分配，需要创建RolePermission实体和Repository
        // 这里先留空，后续可以扩展
    }
}

