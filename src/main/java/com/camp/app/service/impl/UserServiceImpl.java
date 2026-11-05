package com.camp.app.service.impl;

import com.camp.app.api.BizException;
import com.camp.app.api.PageResult;
import com.camp.app.entity.Student;
import com.camp.app.entity.SysTeacherClass;
import com.camp.app.entity.User;
import com.camp.app.model.DataScope;
import com.camp.app.model.UserVo;
import com.camp.app.repository.ClassCourseRepository;
import com.camp.app.repository.ClassRepository;
import com.camp.app.repository.StudentRepository;
import com.camp.app.repository.SysTeacherClassRepository;
import com.camp.app.repository.UserRepository;
import com.camp.app.security.AuthContext;
import com.camp.app.security.Phase1Roles;
import com.camp.app.service.AccessService;
import com.camp.app.service.AuditLogService;
import com.camp.app.service.FileStorageService;
import com.camp.app.service.UserRoleAdminService;
import com.camp.app.service.UserService;
import com.camp.app.util.PasswordEncoderUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 用户服务实现。编辑接口忽略 password，初始密码只在创建时写入。
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private static final int USER_ACCOUNT = 7;

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final SysTeacherClassRepository sysTeacherClassRepository;
    private final ClassCourseRepository classCourseRepository;
    private final ClassRepository classRepository;
    private final AccessService accessService;
    private final UserRoleAdminService userRoleAdminService;
    private final AuditLogService auditLogService;
    private final FileStorageService fileStorageService;

    @Override
    public PageResult<UserVo> list(long page, long size, Integer userType, Integer status, String keyword) {
        Pageable pageable = PageRequest.of((int) (page - 1), (int) size);
        Set<Long> visible = visibleUserIds();
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (visible != null) {
                if (visible.isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(root.get("id").in(visible));
                }
            }
            if (userType != null) {
                predicates.add(cb.equal(root.get("userType"), userType));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(keyword)) {
                String likePattern = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("username"), likePattern),
                        cb.like(root.get("realName"), likePattern)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<User> userPage = userRepository.findAll(spec, pageable);
        List<UserVo> voList = userPage.getContent().stream().map(this::toVo).toList();
        return PageResult.<UserVo>builder()
                .records(voList)
                .total(userPage.getTotalElements())
                .page(page)
                .size(size)
                .build();
    }

    @Override
    public UserVo detail(Long id) {
        User user = require(id);
        assertCanRead(user);
        return toVo(user);
    }

    @Override
    @Transactional
    public UserVo create(UserVo userVo) {
        if (userVo == null || !StringUtils.hasText(userVo.getUsername())) {
            throw BizException.badRequest("用户名不能为空");
        }
        if (!StringUtils.hasText(userVo.getRealName())) {
            throw BizException.badRequest("姓名不能为空");
        }
        String username = userVo.getUsername().trim();
        if (username.length() < 3 || username.length() > 50 || username.chars().anyMatch(Character::isWhitespace)) {
            throw BizException.unprocessable("用户名需为 3-50 位且不含空白");
        }
        if (userVo.getRealName().trim().length() > 50) {
            throw BizException.unprocessable("姓名不能超过 50 字");
        }
        Integer userType = userVo.getUserType() == null ? 1 : userVo.getUserType();
        if (userType < 1 || userType > 3) {
            throw BizException.unprocessable("userType 只能是 1 学生、2 教职工、3 管理员");
        }
        Integer status = userVo.getStatus() == null ? 1 : userVo.getStatus();
        if (status != 0 && status != 1) {
            throw BizException.unprocessable("status 只能是 0 或 1");
        }
        validateContact(userVo);
        assertCanCreate(userVo, userType);
        if (userRepository.findByUsername(username).isPresent()) {
            throw BizException.conflict("用户名已存在");
        }
        fileStorageService.assertReferenced(userVo.getAvatarUrl());

        User user = new User();
        user.setUsername(username);
        user.setRealName(userVo.getRealName().trim());
        user.setUserType(userType);
        user.setStatus(status);
        user.setIdCard(blankToNull(userVo.getIdCard()));
        user.setPhone(blankToNull(userVo.getPhone()));
        user.setEmail(blankToNull(userVo.getEmail()));
        user.setAvatarUrl(blankToNull(userVo.getAvatarUrl()));
        user.setPassword(PasswordEncoderUtil.encode(initialPassword(userVo)));
        user = userRepository.save(user);
        userRoleAdminService.replaceRoleCodes(user.getId(), resolveRoles(userVo, userType));
        auditLogService.recordEvent(USER_ACCOUNT, AuthContext.userId(), user.getId(), "USER", String.valueOf(user.getId()),
                "create username=" + username + " userType=" + userType);
        return toVo(user);
    }

    @Override
    @Transactional
    public UserVo update(Long id, UserVo userVo) {
        User user = require(id);
        assertCanManage(user);
        if (userVo == null) {
            throw BizException.badRequest("请求体不能为空");
        }
        if (StringUtils.hasText(userVo.getUsername()) && !user.getUsername().equals(userVo.getUsername().trim())) {
            String username = userVo.getUsername().trim();
            if (username.length() < 3 || username.length() > 50) {
                throw BizException.unprocessable("用户名需为 3-50 位");
            }
            if (userRepository.findByUsername(username).isPresent()) {
                throw BizException.conflict("用户名已存在");
            }
            user.setUsername(username);
        }
        if (StringUtils.hasText(userVo.getRealName())) {
            if (userVo.getRealName().trim().length() > 50) {
                throw BizException.unprocessable("姓名不能超过 50 字");
            }
            user.setRealName(userVo.getRealName().trim());
        }
        validateContact(userVo);
        if (StringUtils.hasText(userVo.getIdCard())) {
            user.setIdCard(userVo.getIdCard().trim());
        }
        if (StringUtils.hasText(userVo.getPhone())) {
            user.setPhone(userVo.getPhone().trim());
        }
        if (StringUtils.hasText(userVo.getEmail())) {
            user.setEmail(userVo.getEmail().trim());
        }
        if (userVo.getUserType() != null) {
            if (userVo.getUserType() < 1 || userVo.getUserType() > 3) {
                throw BizException.unprocessable("userType 只能是 1 学生、2 教职工、3 管理员");
            }
            assertCanAssignType(userVo.getUserType());
            user.setUserType(userVo.getUserType());
        }
        if (userVo.getStatus() != null) {
            if (userVo.getStatus() != 0 && userVo.getStatus() != 1) {
                throw BizException.unprocessable("status 只能是 0 或 1");
            }
            user.setStatus(userVo.getStatus());
        }
        if (userVo.getAvatarUrl() != null) {
            fileStorageService.assertReferenced(userVo.getAvatarUrl());
            user.setAvatarUrl(blankToNull(userVo.getAvatarUrl()));
        }
        if (userVo.getRoleCodes() != null) {
            List<String> roles = Phase1Roles.normalize(userVo.getRoleCodes());
            assertCanAssignRoles(roles);
            if (roles.isEmpty()) {
                throw BizException.badRequest("roleCodes 含无法识别的角色");
            }
            userRoleAdminService.replaceRoleCodes(id, roles);
        }
        userRepository.save(user);
        auditLogService.recordEvent(USER_ACCOUNT, AuthContext.userId(), id, "USER", String.valueOf(id), "update");
        return toVo(user);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        User user = require(id);
        assertCanManage(user);
        if (id.equals(AuthContext.userId())) {
            throw BizException.unprocessable("不能删除当前登录账号");
        }
        if (studentRepository.findByUserId(id).isPresent()
                || !classCourseRepository.findByTeacher_Id(id).isEmpty()
                || !classRepository.findByHeadTeacherId(id).isEmpty()) {
            throw BizException.conflict("账号已被学籍或排课引用，请改为停用");
        }
        userRepository.delete(user);
        auditLogService.recordEvent(USER_ACCOUNT, AuthContext.userId(), id, "USER", String.valueOf(id), "delete username=" + user.getUsername());
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw BizException.unprocessable("status 只能是 0 或 1");
        }
        User user = require(id);
        assertCanManage(user);
        if (id.equals(AuthContext.userId()) && status == 0) {
            throw BizException.unprocessable("不能停用当前登录账号");
        }
        user.setStatus(status);
        userRepository.save(user);
        auditLogService.recordEvent(USER_ACCOUNT, AuthContext.userId(), id, "USER", String.valueOf(id), "status=" + status);
    }

    private User require(Long id) {
        return userRepository.findById(id).orElseThrow(() -> BizException.notFound("用户不存在"));
    }

    private String initialPassword(UserVo userVo) {
        if (StringUtils.hasText(userVo.getPassword())) {
            if (userVo.getPassword().length() < 6) {
                throw BizException.unprocessable("初始密码至少 6 位");
            }
            return userVo.getPassword();
        }
        return "123456";
    }

    private void validateContact(UserVo userVo) {
        if (userVo.getPhone() != null && userVo.getPhone().length() > 20) {
            throw BizException.unprocessable("手机号过长");
        }
        if (userVo.getIdCard() != null && userVo.getIdCard().length() > 18) {
            throw BizException.unprocessable("身份证号过长");
        }
        if (StringUtils.hasText(userVo.getEmail()) && (!userVo.getEmail().contains("@") || userVo.getEmail().length() > 100)) {
            throw BizException.unprocessable("邮箱格式不正确");
        }
    }

    private List<String> resolveRoles(UserVo userVo, int userType) {
        if (userVo.getRoleCodes() != null && !userVo.getRoleCodes().isEmpty()) {
            List<String> roles = Phase1Roles.normalize(userVo.getRoleCodes());
            if (roles.isEmpty()) {
                throw BizException.badRequest("仅支持 ADMIN、ACADEMIC、TEACHER、STUDENT");
            }
            assertCanAssignRoles(roles);
            return roles;
        }
        String code = switch (userType) {
            case 3 -> Phase1Roles.ADMIN;
            case 2 -> Phase1Roles.TEACHER;
            default -> Phase1Roles.STUDENT;
        };
        assertCanAssignRoles(List.of(code));
        return List.of(code);
    }

    private void assertCanCreate(UserVo userVo, int userType) {
        DataScope scope = accessService.current();
        if (scope.isAll()) {
            return;
        }
        if (!"GRADE".equals(scope.getLevel())) {
            throw BizException.forbidden("超出账号管理范围");
        }
        if (userType == 3) {
            throw BizException.forbidden("教务不能创建管理员");
        }
        if (userVo.getRoleCodes() != null) {
            assertCanAssignRoles(Phase1Roles.normalize(userVo.getRoleCodes()));
        }
    }

    private void assertCanAssignType(int userType) {
        DataScope scope = accessService.current();
        if (scope.isAll()) {
            return;
        }
        if (userType == 3) {
            throw BizException.forbidden("教务不能把账号改为管理员");
        }
    }

    private void assertCanAssignRoles(List<String> roles) {
        DataScope scope = accessService.current();
        if (scope.isAll() || roles == null) {
            return;
        }
        if (roles.contains(Phase1Roles.ADMIN) || roles.contains(Phase1Roles.ACADEMIC)) {
            throw BizException.forbidden("教务只能分配教师或学生角色");
        }
    }

    private void assertCanRead(User user) {
        DataScope scope = accessService.current();
        if (scope.isAll() || (scope.getUserId() != null && scope.getUserId().equals(user.getId()))) {
            return;
        }
        assertCanManage(user);
    }

    private void assertCanManage(User user) {
        DataScope scope = accessService.current();
        if (scope.isAll()) {
            return;
        }
        if (!"GRADE".equals(scope.getLevel())) {
            throw BizException.forbidden("超出账号管理范围");
        }
        List<String> roles = accessService.rolesOf(user);
        if (roles.contains(Phase1Roles.ADMIN) || roles.contains(Phase1Roles.ACADEMIC)) {
            throw BizException.forbidden("教务不能管理管理员或教务账号");
        }
        if (visibleUserIds().contains(user.getId())) {
            return;
        }
        throw BizException.forbidden("超出账号管理范围");
    }

    /**
     * null 表示不限制（管理员全校）。教务看到授权范围内的学生、任课教师，以及尚未绑定班级的教师/学生账号。
     */
    private Set<Long> visibleUserIds() {
        DataScope scope = accessService.current();
        if (scope.isAll()) {
            return null;
        }
        if ("SELF".equals(scope.getLevel()) || "CLASS".equals(scope.getLevel())) {
            return scope.getUserId() == null ? Set.of() : Set.of(scope.getUserId());
        }
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        List<Long> classIds = scope.getClassIds() == null ? List.of() : scope.getClassIds();
        if (!classIds.isEmpty()) {
            for (Student student : studentRepository.findByClassIdIn(classIds)) {
                if (student.getUserId() != null && !Boolean.TRUE.equals(student.getIsDeleted())) {
                    ids.add(student.getUserId());
                }
            }
            for (SysTeacherClass link : sysTeacherClassRepository.findByClassIdIn(classIds)) {
                ids.add(link.getTeacherId());
            }
        }
        for (User user : userRepository.findAll()) {
            if (user.getUserType() != null && user.getUserType() == 3) {
                continue;
            }
            boolean boundStudent = studentRepository.findByUserId(user.getId()).isPresent();
            boolean boundTeacher = !sysTeacherClassRepository.findByTeacherId(user.getId()).isEmpty();
            if (boundStudent || boundTeacher) {
                continue;
            }
            List<String> roles = accessService.rolesOf(user);
            if (roles.contains(Phase1Roles.ADMIN) || roles.contains(Phase1Roles.ACADEMIC)) {
                continue;
            }
            ids.add(user.getId());
        }
        return ids;
    }

    private UserVo toVo(User user) {
        UserVo vo = new UserVo();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setIdCard(user.getIdCard());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setUserType(user.getUserType());
        vo.setStatus(user.getStatus());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setUpdatedAt(user.getUpdatedAt());
        vo.setRoleCodes(userRoleAdminService.roleCodes(user.getId()));
        vo.setPassword(null);
        return vo;
    }

    private String blankToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
