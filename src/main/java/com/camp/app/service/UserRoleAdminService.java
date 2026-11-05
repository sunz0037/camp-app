package com.camp.app.service;

import java.util.List;

public interface UserRoleAdminService {
    List<String> roleCodes(Long userId);

    void replaceRoleCodes(Long userId, List<String> roleCodes);

    void replacePermissionCodes(Long roleId, List<String> permissionCodes);
}
