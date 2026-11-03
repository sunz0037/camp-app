package com.camp.app.service.impl;

import com.camp.app.api.BizException;
import com.camp.app.entity.Role;
import com.camp.app.entity.RolePermissionGrant;
import com.camp.app.entity.UserRole;
import com.camp.app.repository.RolePermissionGrantRepository;
import com.camp.app.repository.RoleRepository;
import com.camp.app.repository.UserRepository;
import com.camp.app.repository.UserRoleRepository;
import com.camp.app.security.Phase1Permissions;
import com.camp.app.security.Phase1Roles;
import com.camp.app.service.UserRoleAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserRoleAdminServiceImpl implements UserRoleAdminService {
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionGrantRepository rolePermissionGrantRepository;

    @Override
    @Transactional(readOnly = true)
    public List<String> roleCodes(Long userId) {
        userRepository.findById(userId).orElseThrow(() -> BizException.notFound("用户不存在"));
        List<String> codes = new ArrayList<>();
        for (UserRole link : userRoleRepository.findByUserId(userId)) {
            roleRepository.findById(link.getRoleId()).map(Role::getRoleCode).ifPresent(codes::add);
        }
        return Phase1Roles.normalize(codes);
    }

    @Override
    @Transactional
    public void replaceRoleCodes(Long userId, List<String> roleCodes) {
        userRepository.findById(userId).orElseThrow(() -> BizException.notFound("用户不存在"));
        if (roleCodes == null || roleCodes.isEmpty()) {
            throw BizException.badRequest("roleCodes 不能为空");
        }
        List<String> normalized = Phase1Roles.normalize(roleCodes);
        if (normalized.isEmpty()) {
            throw BizException.badRequest("仅支持 ADMIN、ACADEMIC、TEACHER、STUDENT");
        }
        userRoleRepository.deleteByUserId(userId);
        for (String code : normalized) {
            Role role = roleRepository.findByRoleCode(code).orElseGet(() -> {
                Role created = new Role();
                created.setRoleCode(code);
                created.setRoleName(code);
                created.setStatus(1);
                created.setDescription("首期四档角色");
                return roleRepository.save(created);
            });
            UserRole link = new UserRole();
            link.setUserId(userId);
            link.setRoleId(role.getId());
            userRoleRepository.save(link);
        }
    }

    @Override
    @Transactional
    public void replacePermissionCodes(Long roleId, List<String> permissionCodes) {
        Role role = roleRepository.findById(roleId).orElseThrow(() -> BizException.notFound("角色不存在"));
        if (!Phase1Roles.isPhase1Code(role.getRoleCode()) && !"GRADE_ADMIN".equals(role.getRoleCode())) {
            throw BizException.badRequest("首期只维护四档角色的权限码");
        }
        String roleCode = "GRADE_ADMIN".equals(role.getRoleCode()) ? Phase1Roles.ACADEMIC : role.getRoleCode();
        if (permissionCodes == null) {
            throw BizException.badRequest("permissionCodes 不能为空");
        }
        for (String code : permissionCodes) {
            if (!Phase1Permissions.known(code)) {
                throw BizException.badRequest("未知权限码: " + code);
            }
        }
        rolePermissionGrantRepository.deleteByRoleCode(roleCode);
        for (String code : permissionCodes.stream().distinct().toList()) {
            RolePermissionGrant grant = new RolePermissionGrant();
            grant.setRoleCode(roleCode);
            grant.setPermissionCode(code);
            rolePermissionGrantRepository.save(grant);
        }
    }
}
