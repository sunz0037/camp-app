package com.camp.app.service.impl;

import com.camp.app.api.BizException;
import com.camp.app.api.PageResult;
import com.camp.app.attendance.AttendanceCalculator;
import com.camp.app.entity.ClassAttendanceDaily;
import com.camp.app.entity.Student;
import com.camp.app.entity.StudentAttendanceRecord;
import com.camp.app.model.AttendanceRecordItem;
import com.camp.app.model.AttendanceSummaryRequest;
import com.camp.app.model.ClassAttendanceDailyVo;
import com.camp.app.model.DataScope;
import com.camp.app.model.StudentAttendanceRecordVo;
import com.camp.app.repository.ClassAttendanceDailyRepository;
import com.camp.app.repository.ClassRepository;
import com.camp.app.repository.StudentAttendanceRecordRepository;
import com.camp.app.repository.StudentRepository;
import com.camp.app.repository.UserRepository;
import com.camp.app.security.AuthContext;
import com.camp.app.service.AccessService;
import com.camp.app.service.AttendanceService;
import com.camp.app.util.BeanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {
    private static final Set<String> EXCEPTION_TYPES = Set.of(
            "LATE", "EARLY_LEAVE", "PERSONAL_LEAVE", "SICK_LEAVE", "ABSENT");
    private static final Set<String> CONFIRM_STATUSES = Set.of("PENDING", "CONFIRMED", "CLEARED");

    private final ClassAttendanceDailyRepository classAttendanceDailyRepository;
    private final StudentAttendanceRecordRepository studentAttendanceRecordRepository;
    private final ClassRepository classRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final AccessService accessService;

    @Override
    public PageResult<ClassAttendanceDailyVo> listClassDaily(long page, long size, String grade, String className, String attendanceDate) {
        Pageable pageable = PageRequest.of((int) (page - 1), (int) size);
        LocalDate date = attendanceDate != null ? LocalDate.parse(attendanceDate) : null;
        Page<ClassAttendanceDaily> dailyPage = classAttendanceDailyRepository.findByFilters(grade, className, date, pageable);
        List<ClassAttendanceDailyVo> voList = dailyPage.getContent().stream().map(this::toSummaryVo).toList();
        return PageResult.<ClassAttendanceDailyVo>builder()
                .page(page).size(size).total(dailyPage.getTotalElements()).records(voList).build();
    }

    @Override
    @Transactional
    public void upsertClassDaily(ClassAttendanceDailyVo vo) {
        ClassAttendanceDaily daily = classAttendanceDailyRepository
                .findByAttendanceDateAndGradeAndClassName(vo.getAttendanceDate(), vo.getGrade(), vo.getClassName())
                .orElse(new ClassAttendanceDaily());
        BeanMapper.copyProperties(vo, daily);
        if (vo.getPresentCount() != null) {
            daily.setPresentCount(vo.getPresentCount());
        }
        classAttendanceDailyRepository.save(daily);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ClassAttendanceDailyVo> listSummaries(long page, long size, Long classId, String attendanceDate) {
        DataScope scope = accessService.current();
        Pageable pageable = PageRequest.of((int) (page - 1), (int) size);
        LocalDate date = attendanceDate != null ? LocalDate.parse(attendanceDate) : null;
        Page<ClassAttendanceDaily> dailyPage = classAttendanceDailyRepository.findByFilters(null, null, date, pageable);
        List<ClassAttendanceDailyVo> voList = dailyPage.getContent().stream()
                .filter(row -> classId == null || classId.equals(row.getClassId()))
                .filter(row -> inScope(scope, row.getClassId()))
                .map(this::toSummaryVo)
                .toList();
        return PageResult.<ClassAttendanceDailyVo>builder()
                .page(page).size(size).total(voList.size()).records(voList).build();
    }

    @Override
    @Transactional
    public ClassAttendanceDailyVo saveSummary(AttendanceSummaryRequest request) {
        return writeSummary(null, request);
    }

    @Override
    @Transactional
    public ClassAttendanceDailyVo updateSummary(Long id, AttendanceSummaryRequest request) {
        ClassAttendanceDaily existing = classAttendanceDailyRepository.findById(id)
                .orElseThrow(() -> BizException.notFound("考勤汇总不存在"));
        if (request.getClassId() == null) {
            request.setClassId(existing.getClassId());
        }
        if (request.getAttendanceDate() == null) {
            request.setAttendanceDate(existing.getAttendanceDate());
        }
        return writeSummary(existing, request);
    }

    @Override
    @Transactional
    public void deleteSummary(Long id) {
        ClassAttendanceDaily daily = classAttendanceDailyRepository.findById(id)
                .orElseThrow(() -> BizException.notFound("考勤汇总不存在"));
        if (daily.getClassId() != null) {
            classRepository.findById(daily.getClassId()).ifPresent(accessService::assertClassInScope);
            studentAttendanceRecordRepository.deleteByClassIdAndAttendanceDate(daily.getClassId(), daily.getAttendanceDate());
        }
        studentAttendanceRecordRepository.deleteBySummaryId(id);
        classAttendanceDailyRepository.delete(daily);
    }

    @Override
    public PageResult<StudentAttendanceRecordVo> listStudentAttendance(long page, long size, String grade, String className, String attendanceDate, Integer status) {
        Pageable pageable = PageRequest.of((int) (page - 1), (int) size);
        LocalDate date = attendanceDate != null ? LocalDate.parse(attendanceDate) : null;
        Page<StudentAttendanceRecord> recordPage = studentAttendanceRecordRepository.findByFilters(
                grade, className, date, AttendanceCalculator.legacyStatusToCode(status), pageable);
        List<StudentAttendanceRecordVo> voList = recordPage.getContent().stream().map(this::toRecordVo).toList();
        return PageResult.<StudentAttendanceRecordVo>builder()
                .page(page).size(size).total(recordPage.getTotalElements()).records(voList).build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<StudentAttendanceRecordVo> listRecords(long page, long size, Long classId, Long studentId, String attendanceDate) {
        DataScope scope = accessService.current();
        Long effectiveStudentId = studentId;
        if ("SELF".equals(scope.getLevel())) {
            effectiveStudentId = scope.getStudentId();
            if (effectiveStudentId == null) {
                return PageResult.<StudentAttendanceRecordVo>builder().page(page).size(size).total(0).records(List.of()).build();
            }
        }
        Pageable pageable = PageRequest.of((int) (page - 1), (int) size);
        LocalDate date = attendanceDate != null ? LocalDate.parse(attendanceDate) : null;
        Page<StudentAttendanceRecord> recordPage = studentAttendanceRecordRepository.findByFilters(null, null, date, null, pageable);
        Long filterStudent = effectiveStudentId;
        List<StudentAttendanceRecordVo> voList = recordPage.getContent().stream()
                .filter(row -> filterStudent == null || filterStudent.equals(row.getStudentId()))
                .filter(row -> classId == null || classId.equals(classIdOfStudent(row.getStudentId())))
                .filter(row -> "SELF".equals(scope.getLevel()) || inScope(scope, classIdOfStudent(row.getStudentId())))
                .map(this::toRecordVo)
                .toList();
        return PageResult.<StudentAttendanceRecordVo>builder()
                .page(page).size(size).total(voList.size()).records(voList).build();
    }

    @Override
    @Transactional
    public void createStudentAttendance(StudentAttendanceRecordVo vo) {
        applyRecordDefaults(vo);
        StudentAttendanceRecord record = new StudentAttendanceRecord();
        record.setStudentId(vo.getStudentId());
        record.setSummaryId(vo.getSummaryId());
        record.setCourseId(vo.getCourseId());
        record.setAttendanceDate(vo.getAttendanceDate());
        record.setGrade(vo.getGrade());
        record.setClassName(vo.getClassName());
        record.setExceptionType(AttendanceCalculator.codeOf(vo.getExceptionType()));
        record.setConfirmStatus(AttendanceCalculator.confirmCode(vo.getConfirmStatus()));
        record.setReasonType(vo.getReasonType());
        record.setReasonDetail(vo.getReasonDetail());
        record.setRecorderId(AuthContext.userId());
        studentAttendanceRecordRepository.save(record);
    }

    @Override
    @Transactional
    public void updateStudentAttendance(Long id, StudentAttendanceRecordVo vo) {
        StudentAttendanceRecord record = studentAttendanceRecordRepository.findById(id)
                .orElseThrow(() -> BizException.notFound("考勤明细不存在"));
        applyRecordDefaults(vo);
        record.setId(id);
        if (vo.getStudentId() != null) {
            record.setStudentId(vo.getStudentId());
        }
        if (vo.getCourseId() != null) {
            record.setCourseId(vo.getCourseId());
        }
        if (vo.getAttendanceDate() != null) {
            record.setAttendanceDate(vo.getAttendanceDate());
        }
        record.setExceptionType(AttendanceCalculator.codeOf(vo.getExceptionType()));
        record.setConfirmStatus(AttendanceCalculator.confirmCode(vo.getConfirmStatus()));
        if (vo.getReasonDetail() != null) {
            record.setReasonDetail(vo.getReasonDetail());
        }
        studentAttendanceRecordRepository.save(record);
    }

    @Override
    @Transactional
    public void deleteStudentAttendance(Long id) {
        if (!studentAttendanceRecordRepository.existsById(id)) {
            throw BizException.notFound("考勤明细不存在");
        }
        studentAttendanceRecordRepository.deleteById(id);
    }

    private ClassAttendanceDailyVo writeSummary(ClassAttendanceDaily existing, AttendanceSummaryRequest request) {
        if (request.getClassId() == null || request.getAttendanceDate() == null) {
            throw BizException.badRequest("classId 与 attendanceDate 不能为空");
        }
        com.camp.app.entity.Class schoolClass = classRepository.findById(request.getClassId())
                .orElseThrow(() -> BizException.notFound("班级不存在"));
        accessService.assertClassInScope(schoolClass);
        List<AttendanceRecordItem> items = request.getRecords() == null ? List.of() : request.getRecords();
        Set<Long> seen = new HashSet<>();
        for (AttendanceRecordItem item : items) {
            if (item.getStudentId() == null || item.getExceptionType() == null || !EXCEPTION_TYPES.contains(item.getExceptionType())) {
                throw BizException.unprocessable("异常明细需要 studentId 与合法 exceptionType");
            }
            if (!seen.add(item.getStudentId())) {
                throw BizException.unprocessable("同一学生同一天只能有一条异常明细");
            }
            if (item.getConfirmStatus() != null && !CONFIRM_STATUSES.contains(item.getConfirmStatus())) {
                throw BizException.unprocessable("confirmStatus 必须是 PENDING、CONFIRMED 或 CLEARED");
            }
            Student student = studentRepository.findById(item.getStudentId())
                    .orElseThrow(() -> BizException.notFound("学生不存在"));
            if (!request.getClassId().equals(student.getClassId())) {
                throw BizException.unprocessable("学生不属于该班级");
            }
        }
        ClassAttendanceDaily daily = existing;
        if (daily == null) {
            daily = classAttendanceDailyRepository
                    .findByClassIdAndAttendanceDate(request.getClassId(), request.getAttendanceDate())
                    .orElse(new ClassAttendanceDaily());
        }
        AttendanceCalculator.Counts counts = AttendanceCalculator.fromTypes(
                items.stream().map(AttendanceRecordItem::getExceptionType).toList());
        int expected = request.getExpectedCount() != null
                ? request.getExpectedCount()
                : studentRepository.countByClassId(request.getClassId()).intValue();
        int actual = request.getActualCount() != null
                ? request.getActualCount()
                : Math.max(0, expected - counts.absent() - counts.leave());
        daily.setClassId(schoolClass.getId());
        daily.setAttendanceDate(request.getAttendanceDate());
        daily.setGrade(schoolClass.getGrade() == null ? "" : schoolClass.getGrade().getGradeName());
        daily.setClassName(schoolClass.getClassName());
        daily.setExpectedCount(expected);
        daily.setPresentCount(actual);
        daily.setAbsentCount(counts.absent());
        daily.setLateCount(counts.late());
        daily.setLeaveCount(counts.leave());
        daily.setEarlyLeaveCount(counts.earlyLeave());
        daily.setRemark(request.getRemark());
        daily.setRecorderId(AuthContext.userId());
        daily = classAttendanceDailyRepository.save(daily);

        studentAttendanceRecordRepository.deleteByClassIdAndAttendanceDate(schoolClass.getId(), request.getAttendanceDate());
        studentAttendanceRecordRepository.deleteBySummaryId(daily.getId());
        List<StudentAttendanceRecordVo> saved = new ArrayList<>();
        for (AttendanceRecordItem item : items) {
            StudentAttendanceRecord record = new StudentAttendanceRecord();
            record.setStudentId(item.getStudentId());
            record.setSummaryId(daily.getId());
            record.setCourseId(item.getCourseId());
            record.setAttendanceDate(request.getAttendanceDate());
            record.setGrade(schoolClass.getGrade() == null ? null : schoolClass.getGrade().getGradeName());
            record.setClassName(schoolClass.getClassName());
            record.setExceptionType(AttendanceCalculator.codeOf(item.getExceptionType()));
            record.setConfirmStatus(AttendanceCalculator.confirmCode(item.getConfirmStatus()));
            record.setReasonType("SICK_LEAVE".equals(item.getExceptionType()) ? "sick" : item.getExceptionType().toLowerCase());
            record.setReasonDetail(item.getReasonDescription());
            record.setRecorderId(AuthContext.userId());
            studentAttendanceRecordRepository.save(record);
            saved.add(toRecordVo(record));
        }
        ClassAttendanceDailyVo vo = toSummaryVo(daily);
        vo.setRecords(saved);
        return vo;
    }

    private void applyRecordDefaults(StudentAttendanceRecordVo vo) {
        if (vo.getExceptionType() == null && vo.getStatus() != null) {
            vo.setExceptionType(AttendanceCalculator.exceptionTypeOf(vo.getStatus()));
        }
        if (vo.getExceptionType() == null) {
            throw BizException.unprocessable("缺少出勤异常类型");
        }
        if (vo.getConfirmStatus() == null) {
            vo.setConfirmStatus("CONFIRMED");
        }
    }

    private boolean inScope(DataScope scope, Long classId) {
        if (scope.isAll()) {
            return true;
        }
        return classId != null && scope.getClassIds() != null && scope.getClassIds().contains(classId);
    }

    private ClassAttendanceDailyVo toSummaryVo(ClassAttendanceDaily daily) {
        ClassAttendanceDailyVo vo = BeanMapper.map(daily, ClassAttendanceDailyVo.class);
        vo.setPresentCount(daily.getPresentCount());
        vo.setClassId(daily.getClassId());
        vo.setEarlyLeaveCount(daily.getEarlyLeaveCount());
        return vo;
    }

    private Long classIdOfStudent(Long studentId) {
        if (studentId == null) {
            return null;
        }
        return studentRepository.findById(studentId).map(Student::getClassId).orElse(null);
    }

    private StudentAttendanceRecordVo toRecordVo(StudentAttendanceRecord record) {
        StudentAttendanceRecordVo vo = new StudentAttendanceRecordVo();
        vo.setId(record.getId());
        vo.setStudentId(record.getStudentId());
        fillStudentDisplay(vo, record);
        vo.setSummaryId(record.getSummaryId());
        vo.setCourseId(record.getCourseId());
        vo.setAttendanceDate(record.getAttendanceDate());
        vo.setGrade(record.getGrade());
        vo.setExceptionType(AttendanceCalculator.nameOf(record.getExceptionType()));
        vo.setConfirmStatus(AttendanceCalculator.confirmName(record.getConfirmStatus()));
        vo.setStatus(AttendanceCalculator.legacyStatus(vo.getExceptionType()));
        vo.setReasonType(record.getReasonType());
        vo.setReasonDetail(record.getReasonDetail());
        vo.setNotifiedContactId(record.getNotifiedContactId());
        vo.setNotifiedContactName(record.getNotifiedContactName());
        vo.setNotifiedMethod(record.getNotifiedMethod());
        vo.setNotifiedAt(record.getNotifiedAt());
        vo.setAttachmentUrl(record.getAttachmentUrl());
        vo.setRecorderId(record.getRecorderId());
        vo.setCreatedAt(record.getCreatedAt());
        vo.setUpdatedAt(record.getUpdatedAt());
        return vo;
    }

    private void fillStudentDisplay(StudentAttendanceRecordVo vo, StudentAttendanceRecord record) {
        Student student = record.getStudentId() == null ? null : studentRepository.findById(record.getStudentId()).orElse(null);
        if (student != null) {
            vo.setStudentNo(student.getStudentNo());
            if (student.getUserId() != null) {
                userRepository.findById(student.getUserId()).ifPresent(user -> vo.setStudentName(user.getRealName()));
            }
        }
        Long classId = student == null ? null : student.getClassId();
        vo.setClassId(classId);
        String className = null;
        if (classId != null) {
            className = classRepository.findById(classId).map(com.camp.app.entity.Class::getClassName).orElse(null);
        }
        if (className == null || className.isBlank()) {
            className = student == null ? null : student.getClassName();
        }
        if (className == null || className.isBlank()) {
            className = record.getClassName();
        }
        vo.setClassName(className);
    }
}
