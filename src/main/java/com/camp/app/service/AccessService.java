package com.camp.app.service;

import com.camp.app.entity.User;
import com.camp.app.model.DataScope;

import java.util.List;

public interface AccessService {
    List<String> rolesOf(User user);

    List<String> permissionsOf(User user);

    DataScope scopeOf(Long userId);

    DataScope current();

    void assertClassInScope(com.camp.app.entity.Class schoolClass);

    void assertStudentVisible(Long studentId);

    void requireRole(String role);
}
