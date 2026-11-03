package com.camp.app.service.impl;

import com.camp.app.api.BizException;
import com.camp.app.entity.Role;
import com.camp.app.entity.RolePermissionGrant;
import com.camp.app.entity.Student;
import com.camp.app.entity.SysAffairsGrade;
import com.camp.app.entity.SysTeacherClass;
import com.camp.app.entity.User;
import com.camp.app.entity.UserRole;
import com.camp.app.model.DataScope;
import com.camp.app.repository.ClassRepository;
import com.camp.app.repository.RolePermissionGrantRepository;
import com.camp.app.repository.RoleRepository;
import com.camp.app.repository.StudentRepository;
import com.camp.app.repository.SysAffairsGradeRepository;
import com.camp.app.repository.SysTeacherClassRepository;
import com.camp.app.repository.UserRepository;
import com.camp.app.repository.UserRoleRepository;
import com.camp.app.security.AuthContext;
import com.camp.app.security.AuthPrincipal;
import com.camp.app.security.Phase1Permissions;
import com.camp.app.security.Phase1Roles;
import com.camp.app.service.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AccessServiceImpl implements AccessService {
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionGrantRepository rolePermissionGrantRepository;
    private final SysTeacherClassRepository sysTeacherClassRepository;
    private final SysAffairsGradeRepository sysAffairsGradeRepository;
    private final StudentRepository studentRepository;
    private final ClassRepository classRepository;

    @Override
    @Transactional(readOnly = true)
    public List<String> rolesOf(User user) {
        List<String> raw = new ArrayList<>();
        for (UserRole link : userRoleRepository.findByUserId(user.getId())) {
            roleRepository.findById(link.getRoleId()).map(Role::getRoleCode).ifPresent(raw::add);
        }
        List<String> normalized = Phase1Roles.normalize(raw);
        if (normalized.isEmpty()) {
            return List.of(Phase1Roles.fromUserType(user.getUserType()));
        }
        return normalized;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> permissionsOf(User user) {
        List<String> roles = rolesOf(user);
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        for (String role : roles) {
            List<RolePermissionGrant> grants = rolePermissionGrantRepository.findByRoleCode(role);
            if (grants.isEmpty()) {
                codes.addAll(Phase1Permissions.defaultsOf(role));
            } else {
                grants.forEach(grant -> codes.add(grant.getPermissionCode()));
            }
        }
        return new ArrayList<>(codes);
    }

    @Override
    @Transactional(readOnly = true)
    public DataScope scopeOf(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BizException.unauthorized("用户不存在或登录已失效"));
        String highest = Phase1Roles.highest(rolesOf(user));
        if (Phase1Roles.ADMIN.equals(highest)) {
            return DataScope.builder().level("ALL").userId(userId).gradeIds(List.of()).classIds(List.of()).build();
        }
        if (Phase1Roles.ACADEMIC.equals(highest)) {
            List<Long> gradeIds = sysAffairsGradeRepository.findByUserId(userId).stream()
                    .map(SysAffairsGrade::getGradeId).distinct().toList();
            List<Long> classIds = gradeIds.isEmpty() ? List.of() : classRepository.findByGradeIdIn(gradeIds).stream()
                    .map(com.camp.app.entity.Class::getId).toList();
            return DataScope.builder().level("GRADE").userId(userId).gradeIds(gradeIds).classIds(classIds).build();
        }
        if (Phase1Roles.TEACHER.equals(highest)) {
            List<Long> classIds = sysTeacherClassRepository.findByTeacherId(userId).stream()
                    .map(SysTeacherClass::getClassId).distinct().toList();
            List<Long> gradeIds = classIds.stream()
                    .map(id -> classRepository.findById(id).map(c -> c.getGrade() == null ? null : c.getGrade().getId()).orElse(null))
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            return DataScope.builder().level("CLASS").userId(userId).gradeIds(gradeIds).classIds(classIds).build();
        }
        Student student = studentRepository.findByUserId(userId).orElse(null);
        List<Long> classIds = student != null && student.getClassId() != null ? List.of(student.getClassId()) : List.of();
        List<Long> gradeIds = student != null && student.getGradeId() != null ? List.of(student.getGradeId()) : List.of();
        return DataScope.builder()
                .level("SELF")
                .userId(userId)
                .studentId(student == null ? null : student.getId())
                .gradeIds(gradeIds)
                .classIds(classIds)
                .build();
    }

    @Override
    public DataScope current() {
        AuthPrincipal principal = AuthContext.get();
        if (principal != null && principal.devAuth()) {
            return devScope(principal);
        }
        if (principal == null || principal.userId() == null) {
            return DataScope.builder().level("ALL").gradeIds(List.of()).classIds(List.of()).build();
        }
        return scopeOf(principal.userId());
    }

    private DataScope devScope(AuthPrincipal principal) {
        String highest = Phase1Roles.highest(principal.roles());
        Long userId = principal.userId();
        if (Phase1Roles.ADMIN.equals(highest)) {
            return DataScope.builder().level("ALL").userId(userId).gradeIds(List.of()).classIds(List.of()).build();
        }
        if (Phase1Roles.ACADEMIC.equals(highest)) {
            List<Long> gradeIds = principal.devGradeIds() != null ? principal.devGradeIds() : safeGrades(userId);
            List<Long> classIds = principal.devClassIds() != null ? principal.devClassIds() : safeClassesOfGrades(gradeIds);
            return DataScope.builder().level("GRADE").userId(userId).gradeIds(gradeIds).classIds(classIds).build();
        }
        if (Phase1Roles.TEACHER.equals(highest)) {
            List<Long> classIds = principal.devClassIds() != null ? principal.devClassIds() : safeTeacherClasses(userId);
            List<Long> gradeIds = principal.devGradeIds() != null ? principal.devGradeIds() : safeGradesOfClasses(classIds);
            return DataScope.builder().level("CLASS").userId(userId).gradeIds(gradeIds).classIds(classIds).build();
        }
        Student student = safeStudent(userId);
        Long studentId = student == null ? null : student.getId();
        List<Long> classIds = principal.devClassIds() != null
                ? principal.devClassIds()
                : (student != null && student.getClassId() != null ? List.of(student.getClassId()) : List.of());
        List<Long> gradeIds = principal.devGradeIds() != null
                ? principal.devGradeIds()
                : (student != null && student.getGradeId() != null ? List.of(student.getGradeId()) : List.of());
        return DataScope.builder().level("SELF").userId(userId).studentId(studentId).gradeIds(gradeIds).classIds(classIds).build();
    }

    private List<Long> safeGrades(Long userId) {
        try {
            return sysAffairsGradeRepository.findByUserId(userId).stream().map(SysAffairsGrade::getGradeId).distinct().toList();
        } catch (DataAccessException ex) {
            return List.of();
        }
    }

    private List<Long> safeTeacherClasses(Long userId) {
        try {
            return sysTeacherClassRepository.findByTeacherId(userId).stream().map(SysTeacherClass::getClassId).distinct().toList();
        } catch (DataAccessException ex) {
            return List.of();
        }
    }

    private List<Long> safeClassesOfGrades(List<Long> gradeIds) {
        if (gradeIds == null || gradeIds.isEmpty()) {
            return List.of();
        }
        try {
            return classRepository.findByGradeIdIn(gradeIds).stream().map(com.camp.app.entity.Class::getId).toList();
        } catch (DataAccessException ex) {
            return List.of();
        }
    }

    private List<Long> safeGradesOfClasses(List<Long> classIds) {
        if (classIds == null || classIds.isEmpty()) {
            return List.of();
        }
        try {
            return classIds.stream()
                    .map(id -> classRepository.findById(id).map(c -> c.getGrade() == null ? null : c.getGrade().getId()).orElse(null))
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .toList();
        } catch (DataAccessException ex) {
            return List.of();
        }
    }

    private Student safeStudent(Long userId) {
        try {
            return studentRepository.findByUserId(userId).orElse(null);
        } catch (DataAccessException ex) {
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void assertClassInScope(com.camp.app.entity.Class schoolClass) {
        DataScope scope = current();
        if (scope.isAll()) {
            return;
        }
        if (schoolClass != null && scope.getClassIds() != null && scope.getClassIds().contains(schoolClass.getId())) {
            return;
        }
        Long gradeId = schoolClass != null && schoolClass.getGrade() != null ? schoolClass.getGrade().getId() : null;
        if ("GRADE".equals(scope.getLevel()) && gradeId != null && scope.getGradeIds() != null && scope.getGradeIds().contains(gradeId)) {
            return;
        }
        throw BizException.forbidden("超出数据范围");
    }

    @Override
    @Transactional(readOnly = true)
    public void assertStudentVisible(Long studentId) {
        DataScope scope = current();
        if (scope.isAll()) {
            return;
        }
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> BizException.notFound("学生不存在"));
        if ("SELF".equals(scope.getLevel())) {
            if (!student.getId().equals(scope.getStudentId())) {
                throw BizException.forbidden("只能查看本人学籍");
            }
            return;
        }
        if (student.getClassId() != null && scope.getClassIds() != null && scope.getClassIds().contains(student.getClassId())) {
            return;
        }
        if ("GRADE".equals(scope.getLevel()) && student.getGradeId() != null && scope.getGradeIds() != null
                && scope.getGradeIds().contains(student.getGradeId())) {
            return;
        }
        throw BizException.forbidden("超出数据范围");
    }

    @Override
    public void requireRole(String role) {
        AuthPrincipal principal = AuthContext.get();
        if (principal == null || principal.roles() == null || !principal.roles().contains(role)) {
            throw BizException.forbidden("需要角色 " + role);
        }
    }
}
