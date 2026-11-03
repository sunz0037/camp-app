package com.camp.app.service.impl;

import com.camp.app.model.PermissionVo;
import com.camp.app.security.Phase1Permissions;
import com.camp.app.service.PermissionService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PermissionServiceImpl implements PermissionService {
    private static final Map<String, String> NAMES = Map.ofEntries(
            Map.entry(Phase1Permissions.ORG_WRITE, "组织维护"),
            Map.entry(Phase1Permissions.STUDENT_WRITE, "学生维护"),
            Map.entry(Phase1Permissions.STUDENT_IMPORT, "学生导入"),
            Map.entry(Phase1Permissions.STUDENT_EXPORT, "学生导出"),
            Map.entry(Phase1Permissions.COURSE_WRITE, "课程维护"),
            Map.entry(Phase1Permissions.CLASS_COURSE_WRITE, "班级授课维护"),
            Map.entry(Phase1Permissions.ATTENDANCE_RECORD, "考勤登记"),
            Map.entry(Phase1Permissions.ANNOUNCEMENT_PUBLISH, "公告发布"),
            Map.entry(Phase1Permissions.ANNOUNCEMENT_TOP, "公告置顶"),
            Map.entry(Phase1Permissions.ANNOUNCEMENT_WITHDRAW, "公告撤回"),
            Map.entry(Phase1Permissions.USER_WRITE, "用户维护"),
            Map.entry(Phase1Permissions.ROLE_WRITE, "角色维护")
    );

    @Override
    public List<PermissionVo> listAll() {
        return Phase1Permissions.ALL.stream().map(this::leaf).toList();
    }

    public List<PermissionVo> tree() {
        Map<String, PermissionVo> groups = new LinkedHashMap<>();
        for (String code : Phase1Permissions.ALL) {
            String groupName = code.substring(0, code.indexOf(':'));
            PermissionVo group = groups.computeIfAbsent(groupName, key -> {
                PermissionVo node = new PermissionVo();
                node.setPermissionCode(key);
                node.setPermissionName(key);
                node.setResourceType("menu");
                node.setChildren(new ArrayList<>());
                node.setStatus(1);
                return node;
            });
            group.getChildren().add(leaf(code));
        }
        return new ArrayList<>(groups.values());
    }

    private PermissionVo leaf(String code) {
        PermissionVo vo = new PermissionVo();
        vo.setPermissionCode(code);
        vo.setPermissionName(NAMES.getOrDefault(code, code));
        vo.setResourceType("button");
        vo.setStatus(1);
        return vo;
    }
}
