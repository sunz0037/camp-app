package com.camp.app.service.impl;

import com.camp.app.api.BizException;
import com.camp.app.entity.EmergencyContact;
import com.camp.app.entity.Student;
import com.camp.app.entity.User;
import com.camp.app.model.Phase2ResetPasswordRequest;
import com.camp.app.phase2.Phase2Codes;
import com.camp.app.repository.EmergencyContactRepository;
import com.camp.app.repository.StudentRepository;
import com.camp.app.repository.UserRepository;
import com.camp.app.service.AuditLogService;
import com.camp.app.service.Phase2PasswordResetService;
import com.camp.app.util.PasswordEncoderUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class Phase2PasswordResetServiceImpl implements Phase2PasswordResetService {
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final EmergencyContactRepository emergencyContactRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public void reset(Phase2ResetPasswordRequest request) {
        if (request == null) {
            throw BizException.badRequest("请求体不能为空");
        }
        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            throw BizException.badRequest("新密码长度不能少于6位");
        }
        if (request.getIdentityType() == null) {
            throw BizException.badRequest("身份类型不能为空");
        }
        if (request.getIdentityType() == 1) {
            resetStudent(request);
            return;
        }
        if (request.getIdentityType() == 2) {
            resetStaff(request);
            return;
        }
        throw BizException.badRequest("身份类型只能是 1 或 2");
    }

    private void resetStudent(Phase2ResetPasswordRequest request) {
        require(request.getIdentifier(), "学号不能为空");
        require(request.getRealName(), "真实姓名不能为空");
        require(request.getIdCard(), "身份证号不能为空");
        require(request.getContactName(), "应急联系人姓名不能为空");
        require(request.getContactPhone(), "应急联系人电话不能为空");
        require(request.getRelationship(), "关系不能为空");
        Student student = studentRepository.findByStudentNo(request.getIdentifier().trim())
                .orElseThrow(() -> BizException.badRequest("学号不存在"));
        if (student.getUserId() == null) {
            throw BizException.badRequest("用户信息不存在");
        }
        User user = userRepository.findById(student.getUserId())
                .orElseThrow(() -> BizException.badRequest("用户信息不存在"));
        if (!user.getRealName().equals(request.getRealName().trim())
                || user.getIdCard() == null
                || !user.getIdCard().equals(request.getIdCard().trim())) {
            auditLogService.record("PASSWORD_RESET", null, "USER", String.valueOf(user.getId()), "学生六要素校验失败", false);
            throw BizException.badRequest("身份信息不匹配");
        }
        EmergencyContact contact = emergencyContactRepository
                .findByStudentIdAndContactNameAndPhoneAndRelationship(
                        student.getId(),
                        request.getContactName().trim(),
                        request.getContactPhone().trim(),
                        request.getRelationship().trim())
                .orElse(null);
        if (contact == null) {
            auditLogService.record("PASSWORD_RESET", null, "USER", String.valueOf(user.getId()), "学生六要素校验失败", false);
            throw BizException.badRequest("应急联系人信息不匹配");
        }
        user.setPassword(PasswordEncoderUtil.encode(request.getNewPassword()));
        userRepository.save(user);
        auditLogService.record("PASSWORD_RESET", null, "USER", String.valueOf(user.getId()), "学生六要素重置密码", true);
    }

    private void resetStaff(Phase2ResetPasswordRequest request) {
        require(request.getIdentifier(), "工号不能为空");
        require(request.getRealName(), "真实姓名不能为空");
        require(request.getPhone(), "手机号不能为空");
        require(request.getVerificationCode(), "验证码不能为空");
        User user = userRepository.findByUsername(request.getIdentifier().trim())
                .orElseThrow(() -> BizException.badRequest("工号不存在"));
        if (user.getUserType() != null && user.getUserType() == 1) {
            throw BizException.badRequest("请使用学生身份找回");
        }
        if (!user.getRealName().equals(request.getRealName().trim())
                || user.getPhone() == null
                || !user.getPhone().equals(request.getPhone().trim())) {
            auditLogService.record("PASSWORD_RESET", null, "USER", String.valueOf(user.getId()), "教工身份校验失败", false);
            throw BizException.badRequest("身份信息不匹配");
        }
        if (!Phase2Codes.DEV_RESET_CODE.equals(request.getVerificationCode().trim())) {
            auditLogService.record("PASSWORD_RESET", null, "USER", String.valueOf(user.getId()), "教工验证码错误", false);
            throw BizException.badRequest("验证码错误");
        }
        user.setPassword(PasswordEncoderUtil.encode(request.getNewPassword()));
        userRepository.save(user);
        auditLogService.record("PASSWORD_RESET", null, "USER", String.valueOf(user.getId()), "教工验证码重置密码", true);
    }

    private void require(String raw, String message) {
        if (raw == null || raw.isBlank()) {
            throw BizException.badRequest(message);
        }
    }
}
