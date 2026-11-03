package com.camp.app.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 角色按钮权限码。无记录时回退到 Phase1Permissions 默认集。
 * DDL 以数据库任务为准（预期 db/phase1/）。
 */
@Data
@Entity
@Table(name = "role_permission_grants", uniqueConstraints = @UniqueConstraint(name = "uk_role_perm_code", columnNames = {"role_code", "permission_code"}))
public class RolePermissionGrant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_code", nullable = false, length = 50)
    private String roleCode;

    @Column(name = "permission_code", nullable = false, length = 100)
    private String permissionCode;
}
