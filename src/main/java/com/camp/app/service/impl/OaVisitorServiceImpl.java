package com.camp.app.service.impl;

import com.camp.app.api.BizException;
import com.camp.app.api.PageResult;
import com.camp.app.entity.OaVisitor;
import com.camp.app.entity.OaVisitorLog;
import com.camp.app.entity.User;
import com.camp.app.model.OaVisitorVo;
import com.camp.app.phase2.Phase2Access;
import com.camp.app.phase2.Phase2Codes;
import com.camp.app.repository.OaVisitorLogRepository;
import com.camp.app.repository.OaVisitorRepository;
import com.camp.app.repository.UserRepository;
import com.camp.app.service.OaVisitorService;
import com.camp.app.util.Masking;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class OaVisitorServiceImpl implements OaVisitorService {
    private static final String PENDING = "PENDING";
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";
    private static final String CHECKED_IN = "CHECKED_IN";
    private static final String CHECKED_OUT = "CHECKED_OUT";

    private final OaVisitorRepository visitorRepository;
    private final OaVisitorLogRepository logRepository;
    private final UserRepository userRepository;
    private final Phase2Access access;

    @Override
    @Transactional(readOnly = true)
    public PageResult<OaVisitorVo> list(long page, long size, String status, Long hostUserId, String keyword) {
        String role = access.role();
        assertNotStudent(role);
        Long userId = access.userId();
        String statusCode = Phase2Codes.visitorStatus(status);
        String key = lower(keyword);
        List<OaVisitor> visible = visitorRepository.findAll().stream()
                .filter(row -> visible(row, role, userId))
                .filter(row -> statusCode == null || statusCode.equals(row.getStatus()))
                .filter(row -> hostUserId == null || hostUserId.equals(row.getHostUserId()))
                .filter(row -> key == null || hit(row, key))
                .sorted(Comparator.comparing(OaVisitor::getExpectedArrive, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(OaVisitor::getId, Comparator.reverseOrder()))
                .toList();
        int from = (int) Math.min((page - 1) * size, visible.size());
        int to = (int) Math.min(from + size, visible.size());
        List<OaVisitorVo> items = visible.subList(from, to).stream().map(row -> toVo(row, true)).toList();
        return PageResult.<OaVisitorVo>builder().page(page).size(size).total(visible.size()).records(items).build();
    }

    @Override
    @Transactional(readOnly = true)
    public OaVisitorVo get(Long id) {
        return toVo(requiredVisible(id), false);
    }

    @Override
    @Transactional
    public OaVisitorVo create(OaVisitorVo vo) {
        if (vo == null) {
            throw BizException.badRequest("请求体不能为空");
        }
        String role = access.role();
        assertNotStudent(role);
        if (vo.getStatus() != null && !vo.getStatus().isBlank()
                && !PENDING.equalsIgnoreCase(vo.getStatus().trim())) {
            throw BizException.badRequest("新建来访只能是待审，请使用状态接口流转");
        }
        OaVisitor row = new OaVisitor();
        fill(row, vo, true);
        row.setStatus(PENDING);
        row.setCreatedBy(access.userId());
        row = visitorRepository.save(row);
        log(row.getId(), "SUBMIT", null);
        return toVo(row, false);
    }

    @Override
    @Transactional
    public OaVisitorVo update(Long id, OaVisitorVo vo) {
        if (vo == null) {
            throw BizException.badRequest("请求体不能为空");
        }
        OaVisitor row = requiredVisible(id);
        String role = access.role();
        if (access.isTeacher(role)) {
            if (!access.userId().equals(row.getCreatedBy()) || !PENDING.equals(row.getStatus())) {
                throw BizException.forbidden("教师只能修改本人登记的待审来访");
            }
        } else if (!canModerate(role)) {
            throw BizException.forbidden("无权限");
        }
        fill(row, vo, false);
        return toVo(visitorRepository.save(row), false);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        OaVisitor row = requiredVisible(id);
        String role = access.role();
        boolean teacherOwnPending = access.isTeacher(role)
                && access.userId().equals(row.getCreatedBy())
                && PENDING.equals(row.getStatus());
        if (!canModerate(role) && !teacherOwnPending) {
            throw BizException.forbidden("教师只能删除本人登记的待审来访");
        }
        logRepository.deleteByVisitorId(id);
        visitorRepository.delete(row);
    }

    @Override
    @Transactional
    public OaVisitorVo approve(Long id, String remark) {
        return transit(id, PENDING, APPROVED, "APPROVE", remark);
    }

    @Override
    @Transactional
    public OaVisitorVo reject(Long id, String remark) {
        return transit(id, PENDING, REJECTED, "REJECT", remark);
    }

    @Override
    @Transactional
    public OaVisitorVo checkIn(Long id, String remark) {
        return transit(id, APPROVED, CHECKED_IN, "CHECK_IN", remark);
    }

    @Override
    @Transactional
    public OaVisitorVo checkOut(Long id, String remark) {
        return transit(id, CHECKED_IN, CHECKED_OUT, "CHECK_OUT", remark);
    }

    private OaVisitorVo transit(Long id, String from, String to, String action, String remark) {
        assertModerator();
        OaVisitor row = requiredVisible(id);
        if (!from.equals(row.getStatus())) {
            throw BizException.unprocessable("当前状态不能执行该操作");
        }
        row.setStatus(to);
        visitorRepository.save(row);
        log(row.getId(), action, remark);
        return toVo(row, false);
    }

    private void log(Long visitorId, String action, String remark) {
        OaVisitorLog entry = new OaVisitorLog();
        entry.setVisitorId(visitorId);
        entry.setAction(action);
        entry.setOperatorId(access.userId());
        entry.setNote(limit(remark, 255, "备注"));
        logRepository.save(entry);
    }

    private void fill(OaVisitor row, OaVisitorVo vo, boolean creating) {
        if (creating || vo.getVisitorName() != null) {
            row.setName(required(vo.getVisitorName(), "访客姓名", 50));
        }
        if (creating || vo.getPhone() != null) {
            row.setPhone(required(vo.getPhone(), "电话", 20));
        }
        if (creating || vo.getPurpose() != null) {
            row.setPurpose(required(vo.getPurpose(), "事由", 200));
        }
        if (vo.getIdCard() != null) {
            row.setIdNo(limit(vo.getIdCard(), 18, "证件号"));
        }
        if (vo.getOrg() != null) {
            row.setOrg(limit(vo.getOrg(), 100, "单位"));
        }
        if (vo.getHostDept() != null) {
            row.setHostDept(limit(vo.getHostDept(), 100, "被访部门"));
        }
        if (vo.getHostUserId() != null) {
            User host = userRepository.findById(vo.getHostUserId())
                    .orElseThrow(() -> BizException.badRequest("被访人不存在"));
            row.setHostUserId(host.getId());
        }
        if (creating && row.getHostUserId() == null) {
            if (vo.getHostName() != null && !vo.getHostName().isBlank()) {
                throw BizException.badRequest("被访人需要 hostUserId，暂不按姓名解析");
            }
            throw BizException.badRequest("被访人不能为空");
        }
        if (vo.getAppointmentTime() != null) {
            row.setExpectedArrive(vo.getAppointmentTime());
        }
        if (creating && row.getExpectedArrive() == null) {
            throw BizException.badRequest("预约时间不能为空");
        }
        if (vo.getExpectedLeave() != null) {
            row.setExpectedLeave(vo.getExpectedLeave());
        }
    }

    private OaVisitor requiredVisible(Long id) {
        String role = access.role();
        assertNotStudent(role);
        OaVisitor row = visitorRepository.findById(id).orElseThrow(() -> BizException.notFound("来访记录不存在"));
        if (!visible(row, role, access.userId())) {
            throw BizException.forbidden("超出数据范围");
        }
        return row;
    }

    private boolean visible(OaVisitor row, String role, Long userId) {
        if (canModerate(role)) {
            return true;
        }
        return access.isTeacher(role) && userId != null
                && (userId.equals(row.getCreatedBy()) || userId.equals(row.getHostUserId()));
    }

    private void assertNotStudent(String role) {
        if (access.isStudent(role)) {
            throw BizException.forbidden("学生不能查看来访登记");
        }
    }

    private void assertModerator() {
        if (!canModerate(access.role())) {
            throw BizException.forbidden("仅管理员或教务可审批来访");
        }
    }

    private boolean canModerate(String role) {
        return access.isAdmin(role) || access.isAcademic(role);
    }

    private OaVisitorVo toVo(OaVisitor row, boolean mask) {
        OaVisitorVo vo = new OaVisitorVo();
        vo.setId(row.getId());
        vo.setVisitorName(row.getName());
        vo.setIdCard(mask ? Masking.idCard(row.getIdNo()) : row.getIdNo());
        vo.setPhone(mask ? Masking.phone(row.getPhone()) : row.getPhone());
        vo.setOrg(row.getOrg());
        vo.setPurpose(row.getPurpose());
        vo.setHostUserId(row.getHostUserId());
        vo.setHostDept(row.getHostDept());
        if (row.getHostUserId() != null) {
            userRepository.findById(row.getHostUserId()).ifPresent(user -> vo.setHostName(user.getRealName()));
        }
        vo.setAppointmentTime(row.getExpectedArrive());
        vo.setExpectedLeave(row.getExpectedLeave());
        vo.setStatus(row.getStatus());
        vo.setCreatedBy(row.getCreatedBy());
        vo.setCreatedAt(row.getCreatedAt());
        vo.setUpdatedAt(row.getUpdatedAt());
        return vo;
    }

    private boolean hit(OaVisitor row, String key) {
        String hostName = row.getHostUserId() == null ? null
                : userRepository.findById(row.getHostUserId()).map(User::getRealName).orElse(null);
        return contains(row.getName(), key) || contains(hostName, key) || contains(row.getPurpose(), key);
    }

    private boolean contains(String value, String key) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(key);
    }

    private String required(String raw, String label, int max) {
        String value = limit(raw, max, label);
        if (value == null) {
            throw BizException.badRequest(label + "不能为空");
        }
        return value;
    }

    private String limit(String raw, int max, String label) {
        String value = blank(raw);
        if (value != null && value.length() > max) {
            throw BizException.badRequest(label + "过长");
        }
        return value;
    }

    private String blank(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        return value.isEmpty() ? null : value;
    }

    private String lower(String raw) {
        String value = blank(raw);
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }
}
