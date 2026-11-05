package com.camp.app.phase2;

import com.camp.app.api.BizException;
import com.camp.app.model.DataScope;
import com.camp.app.security.AuthContext;
import com.camp.app.security.AuthPrincipal;
import com.camp.app.security.Phase1Roles;
import com.camp.app.service.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Phase2Access {
    private final AccessService accessService;

    public String role() {
        AuthPrincipal principal = AuthContext.get();
        if (principal == null || principal.roles() == null || principal.roles().isEmpty()) {
            throw BizException.unauthorized("未登录");
        }
        return Phase1Roles.highest(principal.roles());
    }

    public DataScope scope() {
        return accessService.current();
    }

    public Long userId() {
        Long userId = AuthContext.userId();
        if (userId == null) {
            throw BizException.unauthorized("未登录");
        }
        return userId;
    }

    public boolean isStudent(String role) {
        return Phase1Roles.STUDENT.equals(role);
    }

    public boolean isTeacher(String role) {
        return Phase1Roles.TEACHER.equals(role);
    }

    public boolean isAcademic(String role) {
        return Phase1Roles.ACADEMIC.equals(role);
    }

    public boolean isAdmin(String role) {
        return Phase1Roles.ADMIN.equals(role);
    }
}
