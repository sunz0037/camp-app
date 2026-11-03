package com.camp.app.repository;

import com.camp.app.entity.RolePermissionGrant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface RolePermissionGrantRepository extends JpaRepository<RolePermissionGrant, Long> {
    List<RolePermissionGrant> findByRoleCode(String roleCode);

    List<RolePermissionGrant> findByRoleCodeIn(Collection<String> roleCodes);

    void deleteByRoleCode(String roleCode);
}
