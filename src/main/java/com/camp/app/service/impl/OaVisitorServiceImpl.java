package com.camp.app.service.impl;

import com.camp.app.api.BizException;
import com.camp.app.api.PageResult;
import com.camp.app.entity.OaVisitor;
import com.camp.app.entity.User;
import com.camp.app.model.OaVisitorVo;
import com.camp.app.phase2.Phase2Access;
import com.camp.app.phase2.Phase2Codes;
import com.camp.app.repository.OaVisitorRepository;
import com.camp.app.repository.UserRepository;
import com.camp.app.service.OaVisitorService;
import com.camp.app.util.Masking;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OaVisitorServiceImpl implements OaVisitorService {
    private static final int PENDING = 0;
    private static final int APPROVED = 1;
    private static final int REJECTED = 2;
    private static final int CHECKED_IN = 3;
    private static final int CHECKED_OUT = 4;

    private final OaVisitorRepository visitorRepository;
    private final UserRepository userRepository;
    private final Phase2Access access;

    @Override
    @Transactional(readOnly = true)
    public PageResult<OaVisitorVo> list(long page, long size, String status, Long hostUserId, String keyword) {
        String role = access.role();
        assertNotStudent(role);
        Long userId = access.userId();
        Integer statusCode = Phase2Codes.visitorStatus(status);
        String key = lower(keyword);
        List<OaVisitor> visible = visitorRepository.findAll().stream()
                .filter(row -> visible(row, role, userId))
                .filter(row -> statusCode == null || statusCode.equals(row.getStatus()))
                .filter(row -> hostUserId == null || hostUserId.equals(row.getHostUserId()))
                .filter(row -> key == null || hit(row, key))
                .sorted(Comparator.comparing(OaVisitor::getAppointmentTime, Comparator.nullsLast(Comparator.reverseOrder()))
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
        OaVisitor row = requiredVisible(id);
        return toVo(row, false);
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
                && !"PENDING".equalsIgnoreCase(vo.getStatus().trim())) {
            throw BizException.badRequest("新建来访只能是待审，请使用状态接口流转");
        }
        OaVisitor row = new OaVisitor();
        row.setVisitNo(blank(vo.getVisitNo()) == null ? newCode("VIS") : vo.getVisitNo().trim());
        if (visitorRepository.findByVisitNo(row.getVisitNo()).isPresent()) {
            throw BizException.conflict("来访单号已存在");
        }
        fill(row, vo, true);
        row.setStatus(PENDING);
        row.setCreatedBy(access.userId());
        return toVo(visitorRepository.save(row), false);
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
            if (!access.userId().equals(row.getCreatedBy()) || row.getStatus() == null || row.getStatus() != PENDING) {
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
        if (canModerate(role)) {
            visitorRepository.delete(row);
            return;
        }
        if (access.isTeacher(role) && access.userId().equals(row.getCreatedBy()) && row.getStatus() != null && row.getStatus() == PENDING) {
            visitorRepository.delete(row);
            return;
        }
        throw BizException.forbidden("教师只能删除本人登记的待审来访");
    }

    @Override
    @Transactional
    public OaVisitorVo approve(Long id, String remark) {
        return transit(id, PENDING, APPROVED, remark, false, false);
    }

    @Override
    @Transactional
    public OaVisitorVo reject(Long id, String remark) {
        return transit(id, PENDING, REJECTED, remark, false, false);
    }

    @Override
    @Transactional
    public OaVisitorVo checkIn(Long id, String remark) {
        return transit(id, APPROVED, CHECKED_IN, remark, true, false);
    }

    @Override
    @Transactional
    public OaVisitorVo checkOut(Long id, String remark) {
        return transit(id, CHECKED_IN, CHECKED_OUT, remark, false, true);
    }

    private OaVisitorVo transit(Long id, int from, int to, String remark, boolean arrive, boolean leave) {
        assertModerator();
        OaVisitor row = requiredVisible(id);
        if (row.getStatus() == null || row.getStatus() != from) {
            throw BizException.unprocessable("当前状态不能执行该操作");
        }
        row.setStatus(to);
        if (arrive) {
            row.setArrivalTime(LocalDateTime.now());
        }
        if (leave) {
            row.setLeaveTime(LocalDateTime.now());
        }
        if (blank(remark) != null) {
            row.setRemark(limit(remark, 500, "备注"));
        }
        return toVo(visitorRepository.save(row), false);
    }

    private void fill(OaVisitor row, OaVisitorVo vo, boolean creating) {
        if (creating || vo.getVisitorName() != null) {
            row.setVisitorName(required(vo.getVisitorName(), "访客姓名", 50));
        }
        if (creating || vo.getPhone() != null) {
            row.setPhone(required(vo.getPhone(), "电话", 20));
        }
        if (creating || vo.getPurpose() != null) {
            row.setPurpose(required(vo.getPurpose(), "事由", 200));
        }
        if (vo.getIdCard() != null) {
            row.setIdCard(limit(vo.getIdCard(), 32, "证件号"));
        }
        if (vo.getHostUserId() != null) {
            User host = userRepository.findById(vo.getHostUserId()).orElseThrow(() -> BizException.badRequest("被访人不存在"));
            row.setHostUserId(host.getId());
            if (vo.getHostName() == null || vo.getHostName().isBlank()) {
                row.setHostName(host.getRealName());
            }
        }
        if (vo.getHostName() != null) {
            row.setHostName(limit(vo.getHostName(), 50, "被访人"));
        }
        if (creating && row.getHostUserId() == null && blank(row.getHostName()) == null) {
            throw BizException.badRequest("被访人不能为空");
        }
        if (vo.getAppointmentTime() != null) {
            row.setAppointmentTime(vo.getAppointmentTime());
        }
        if (creating && row.getAppointmentTime() == null) {
            throw BizException.badRequest("预约时间不能为空");
        }
        if (vo.getRemark() != null) {
            row.setRemark(limit(vo.getRemark(), 500, "备注"));
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
        String role = access.role();
        if (!canModerate(role)) {
            throw BizException.forbidden("仅管理员或教务可审批来访");
        }
    }

    private boolean canModerate(String role) {
        return access.isAdmin(role) || access.isAcademic(role);
    }

    private OaVisitorVo toVo(OaVisitor row, boolean mask) {
        OaVisitorVo vo = new OaVisitorVo();
        vo.setId(row.getId());
        vo.setVisitNo(row.getVisitNo());
        vo.setVisitorName(row.getVisitorName());
        vo.setIdCard(mask ? Masking.idCard(row.getIdCard()) : row.getIdCard());
        vo.setPhone(mask ? Masking.phone(row.getPhone()) : row.getPhone());
        vo.setPurpose(row.getPurpose());
        vo.setHostUserId(row.getHostUserId());
        vo.setHostName(row.getHostName());
        vo.setAppointmentTime(row.getAppointmentTime());
        vo.setArrivalTime(row.getArrivalTime());
        vo.setLeaveTime(row.getLeaveTime());
        vo.setStatus(Phase2Codes.visitorStatusName(row.getStatus()));
        vo.setRemark(row.getRemark());
        vo.setCreatedBy(row.getCreatedBy());
        vo.setCreatedAt(row.getCreatedAt());
        vo.setUpdatedAt(row.getUpdatedAt());
        return vo;
    }

    private boolean hit(OaVisitor row, String key) {
        return contains(row.getVisitorName(), key) || contains(row.getHostName(), key)
                || contains(row.getPurpose(), key) || contains(row.getVisitNo(), key);
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

    private String newCode(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT);
    }
}
