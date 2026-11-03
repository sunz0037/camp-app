package com.camp.app.service.impl;

import com.camp.app.api.BizException;
import com.camp.app.api.PageResult;
import com.camp.app.entity.Course;
import com.camp.app.entity.EduExam;
import com.camp.app.entity.Grade;
import com.camp.app.model.DataScope;
import com.camp.app.model.EduExamVo;
import com.camp.app.phase2.Phase2Access;
import com.camp.app.phase2.Phase2Codes;
import com.camp.app.repository.ClassRepository;
import com.camp.app.repository.CourseRepository;
import com.camp.app.repository.EduExamRepository;
import com.camp.app.repository.EduExamScoreRepository;
import com.camp.app.repository.GradeRepository;
import com.camp.app.service.AccessService;
import com.camp.app.service.EduExamService;
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
public class EduExamServiceImpl implements EduExamService {
    private final EduExamRepository examRepository;
    private final EduExamScoreRepository scoreRepository;
    private final GradeRepository gradeRepository;
    private final ClassRepository classRepository;
    private final CourseRepository courseRepository;
    private final Phase2Access access;
    private final AccessService accessService;

    @Override
    @Transactional(readOnly = true)
    public PageResult<EduExamVo> list(long page, long size, Long gradeId, Long classId, String examType,
                                      Boolean isPublic, String status, String keyword) {
        String role = access.role();
        DataScope scope = access.scope();
        String type = blank(examType) == null ? null : Phase2Codes.examType(examType);
        Integer statusCode = Phase2Codes.examStatus(status);
        String key = lower(keyword);
        List<EduExam> visible = examRepository.findAll().stream()
                .filter(row -> visible(row, scope, role))
                .filter(row -> gradeId == null || gradeId.equals(row.getGradeId()))
                .filter(row -> classId == null || classId.equals(row.getClassId()))
                .filter(row -> type == null || type.equals(row.getExamType()))
                .filter(row -> isPublic == null || isPublic.equals(flag(row.getIsPublic())))
                .filter(row -> statusCode == null || statusCode.equals(row.getStatus()))
                .filter(row -> key == null || hit(row.getExamName(), key) || hit(row.getSubjectName(), key)
                        || hit(row.getExamCode(), key) || hit(row.getRoomNo(), key))
                .sorted(Comparator.comparing(EduExam::getStartTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        return page(visible, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public EduExamVo get(Long id) {
        EduExam exam = required(id);
        if (!visible(exam, access.scope(), access.role())) {
            throw BizException.forbidden("超出数据范围");
        }
        return toVo(exam);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean visibleToCurrentUser(Long id) {
        return examRepository.findById(id).map(exam -> visible(exam, access.scope(), access.role())).orElse(false);
    }

    @Override
    @Transactional
    public EduExamVo create(EduExamVo vo) {
        if (vo == null) {
            throw BizException.badRequest("请求体不能为空");
        }
        EduExam exam = new EduExam();
        exam.setExamCode(blank(vo.getExamCode()) == null ? newCode("EX") : vo.getExamCode().trim());
        if (examRepository.findByExamCode(exam.getExamCode()).isPresent()) {
            throw BizException.conflict("考试编码已存在");
        }
        exam.setExamName(requiredText(vo.getExamName(), "考试名称"));
        exam.setExamType(Phase2Codes.examType(vo.getExamType()));
        if (vo.getStartTime() == null || vo.getEndTime() == null) {
            throw BizException.badRequest("开始时间和结束时间不能为空");
        }
        exam.setStartTime(vo.getStartTime());
        exam.setEndTime(vo.getEndTime());
        exam.setIsPublic(Boolean.TRUE.equals(vo.getIsPublic()) ? 1 : 0);
        exam.setStatus(vo.getStatus() == null || vo.getStatus().isBlank() ? 1 : Phase2Codes.examStatus(vo.getStatus()));
        exam.setGradeId(vo.getGradeId());
        exam.setClassId(vo.getClassId());
        exam.setCourseId(vo.getCourseId());
        exam.setSubjectName(blank(vo.getSubjectName()));
        exam.setRoomNo(limit(vo.getRoomNo(), 32, "考场"));
        exam.setRemark(limit(vo.getRemark(), 500, "备注"));
        exam.setCreatedBy(access.userId());
        align(exam);
        assertTime(exam.getStartTime(), exam.getEndTime());
        assertCanWrite(access.role(), exam.getGradeId(), exam.getClassId());
        return toVo(examRepository.save(exam));
    }

    @Override
    @Transactional
    public EduExamVo update(Long id, EduExamVo vo) {
        if (vo == null) {
            throw BizException.badRequest("请求体不能为空");
        }
        EduExam exam = required(id);
        String role = access.role();
        if (!visible(exam, access.scope(), role)) {
            throw BizException.forbidden("超出数据范围");
        }
        if (blank(vo.getExamCode()) != null && !vo.getExamCode().trim().equals(exam.getExamCode())) {
            String code = vo.getExamCode().trim();
            examRepository.findByExamCode(code).filter(other -> !other.getId().equals(exam.getId()))
                    .ifPresent(other -> {
                        throw BizException.conflict("考试编码已存在");
                    });
            exam.setExamCode(code);
        }
        if (vo.getExamName() != null) {
            exam.setExamName(requiredText(vo.getExamName(), "考试名称"));
        }
        if (vo.getExamType() != null) {
            exam.setExamType(Phase2Codes.examType(vo.getExamType()));
        }
        if (vo.getStartTime() != null) {
            exam.setStartTime(vo.getStartTime());
        }
        if (vo.getEndTime() != null) {
            exam.setEndTime(vo.getEndTime());
        }
        if (vo.getIsPublic() != null) {
            exam.setIsPublic(Boolean.TRUE.equals(vo.getIsPublic()) ? 1 : 0);
        }
        if (vo.getStatus() != null && !vo.getStatus().isBlank()) {
            exam.setStatus(Phase2Codes.examStatus(vo.getStatus()));
        }
        if (vo.getGradeId() != null) {
            exam.setGradeId(vo.getGradeId());
        }
        if (vo.getClassId() != null) {
            exam.setClassId(vo.getClassId());
        }
        if (vo.getCourseId() != null) {
            exam.setCourseId(vo.getCourseId());
        }
        if (vo.getSubjectName() != null) {
            exam.setSubjectName(blank(vo.getSubjectName()));
        }
        if (vo.getRoomNo() != null) {
            exam.setRoomNo(limit(vo.getRoomNo(), 32, "考场"));
        }
        if (vo.getRemark() != null) {
            exam.setRemark(limit(vo.getRemark(), 500, "备注"));
        }
        align(exam);
        assertTime(exam.getStartTime(), exam.getEndTime());
        assertCanWrite(role, exam.getGradeId(), exam.getClassId());
        return toVo(examRepository.save(exam));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        EduExam exam = required(id);
        String role = access.role();
        if (!visible(exam, access.scope(), role)) {
            throw BizException.forbidden("超出数据范围");
        }
        assertCanWrite(role, exam.getGradeId(), exam.getClassId());
        if (scoreRepository.countByExamId(id) > 0) {
            throw BizException.conflict("考试已有成绩，不能删除");
        }
        examRepository.delete(exam);
    }

    private EduExam required(Long id) {
        return examRepository.findById(id).orElseThrow(() -> BizException.notFound("考试不存在"));
    }

    private void align(EduExam exam) {
        if (exam.getClassId() != null) {
            com.camp.app.entity.Class schoolClass = classRepository.findById(exam.getClassId())
                    .orElseThrow(() -> BizException.badRequest("班级不存在"));
            Long gradeId = schoolClass.getGrade() == null ? null : schoolClass.getGrade().getId();
            if (exam.getGradeId() != null && gradeId != null && !exam.getGradeId().equals(gradeId)) {
                throw BizException.badRequest("班级不属于该年级");
            }
            if (exam.getGradeId() == null) {
                exam.setGradeId(gradeId);
            }
        } else if (exam.getGradeId() != null && gradeRepository.findById(exam.getGradeId()).isEmpty()) {
            throw BizException.badRequest("年级不存在");
        }
        if (exam.getCourseId() != null) {
            Course course = courseRepository.findById(exam.getCourseId())
                    .orElseThrow(() -> BizException.badRequest("科目不存在"));
            if (blank(exam.getSubjectName()) == null) {
                exam.setSubjectName(course.getCourseName());
            }
        }
    }

    private void assertCanWrite(String role, Long gradeId, Long classId) {
        if (access.isStudent(role)) {
            throw BizException.forbidden("学生不能维护考试安排");
        }
        if (access.isAdmin(role)) {
            return;
        }
        DataScope scope = access.scope();
        if (access.isTeacher(role)) {
            if (classId == null || scope.getClassIds() == null || !scope.getClassIds().contains(classId)) {
                throw BizException.forbidden("教师只能维护本班考试");
            }
            return;
        }
        if (access.isAcademic(role)) {
            if (classId != null) {
                com.camp.app.entity.Class schoolClass = classRepository.findById(classId)
                        .orElseThrow(() -> BizException.badRequest("班级不存在"));
                accessService.assertClassInScope(schoolClass);
                return;
            }
            if (gradeId != null && scope.getGradeIds() != null && scope.getGradeIds().contains(gradeId)) {
                return;
            }
            throw BizException.forbidden("教务只能维护授权年级内的考试");
        }
        throw BizException.forbidden("无权限");
    }

    private boolean visible(EduExam exam, DataScope scope, String role) {
        if (access.isAdmin(role) || scope.isAll()) {
            return true;
        }
        boolean inOrg = inOrg(exam, scope);
        if (access.isStudent(role)) {
            return flag(exam.getIsPublic()) && (schoolWide(exam) || inOrg);
        }
        if (access.isTeacher(role)) {
            if (exam.getClassId() != null) {
                return scope.getClassIds() != null && scope.getClassIds().contains(exam.getClassId());
            }
            if (exam.getGradeId() != null) {
                return scope.getGradeIds() != null && scope.getGradeIds().contains(exam.getGradeId());
            }
            return flag(exam.getIsPublic());
        }
        return schoolWide(exam) || inOrg;
    }

    private boolean inOrg(EduExam exam, DataScope scope) {
        if (exam.getClassId() != null && scope.getClassIds() != null && scope.getClassIds().contains(exam.getClassId())) {
            return true;
        }
        return exam.getGradeId() != null && scope.getGradeIds() != null && scope.getGradeIds().contains(exam.getGradeId());
    }

    private boolean schoolWide(EduExam exam) {
        return exam.getGradeId() == null && exam.getClassId() == null;
    }

    private EduExamVo toVo(EduExam exam) {
        EduExamVo vo = new EduExamVo();
        vo.setId(exam.getId());
        vo.setExamCode(exam.getExamCode());
        vo.setExamName(exam.getExamName());
        vo.setGradeId(exam.getGradeId());
        vo.setClassId(exam.getClassId());
        vo.setCourseId(exam.getCourseId());
        vo.setSubjectName(exam.getSubjectName());
        vo.setExamType(exam.getExamType());
        vo.setStartTime(exam.getStartTime());
        vo.setEndTime(exam.getEndTime());
        vo.setIsPublic(flag(exam.getIsPublic()));
        vo.setStatus(Phase2Codes.examStatusName(exam.getStatus()));
        vo.setRoomNo(exam.getRoomNo());
        vo.setRemark(exam.getRemark());
        vo.setCreatedBy(exam.getCreatedBy());
        vo.setCreatedAt(exam.getCreatedAt());
        vo.setUpdatedAt(exam.getUpdatedAt());
        if (exam.getGradeId() != null) {
            gradeRepository.findById(exam.getGradeId()).map(Grade::getGradeName).ifPresent(vo::setGradeName);
        }
        if (exam.getClassId() != null) {
            classRepository.findById(exam.getClassId()).map(com.camp.app.entity.Class::getClassName).ifPresent(vo::setClassName);
        }
        return vo;
    }

    private PageResult<EduExamVo> page(List<EduExam> visible, long page, long size) {
        int from = (int) Math.min((page - 1) * size, visible.size());
        int to = (int) Math.min(from + size, visible.size());
        List<EduExamVo> items = visible.subList(from, to).stream().map(this::toVo).toList();
        return PageResult.<EduExamVo>builder().page(page).size(size).total(visible.size()).records(items).build();
    }

    private void assertTime(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw BizException.badRequest("结束时间必须晚于开始时间");
        }
    }

    private boolean flag(Integer value) {
        return value != null && value == 1;
    }

    private String newCode(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT);
    }

    private String requiredText(String raw, String label) {
        String value = blank(raw);
        if (value == null) {
            throw BizException.badRequest(label + "不能为空");
        }
        if (value.length() > 100) {
            throw BizException.badRequest(label + "过长");
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

    private boolean hit(String value, String key) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(key);
    }
}
