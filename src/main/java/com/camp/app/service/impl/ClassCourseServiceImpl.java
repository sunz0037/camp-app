package com.camp.app.service.impl;

import com.camp.app.api.BizException;
import com.camp.app.api.PageResult;
import com.camp.app.entity.ClassCourse;
import com.camp.app.entity.ClassCourseSession;
import com.camp.app.entity.Course;
import com.camp.app.entity.Student;
import com.camp.app.entity.User;
import com.camp.app.entity.Class;
import com.camp.app.model.ClassCourseVo;
import com.camp.app.model.DataScope;
import com.camp.app.model.SessionSlot;
import com.camp.app.model.TimetableItemVo;
import com.camp.app.repository.ClassCourseRepository;
import com.camp.app.repository.ClassCourseSessionRepository;
import com.camp.app.repository.ClassRepository;
import com.camp.app.repository.CourseRepository;
import com.camp.app.repository.StudentRepository;
import com.camp.app.repository.UserRepository;
import com.camp.app.security.AuthContext;
import com.camp.app.security.AuthPrincipal;
import com.camp.app.security.Phase1Roles;
import com.camp.app.service.AccessService;
import com.camp.app.service.AuditLogService;
import com.camp.app.service.ClassCourseService;
import com.camp.app.util.BeanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 班级授课科目关联服务实现
 */
@Service
@RequiredArgsConstructor
public class ClassCourseServiceImpl implements ClassCourseService {
    
    private final ClassCourseRepository classCourseRepository;
    private final ClassRepository classRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final ClassCourseSessionRepository classCourseSessionRepository;
    private final StudentRepository studentRepository;
    private final AccessService accessService;
    private final AuditLogService auditLogService;
    
    @Override
    public List<ClassCourseVo> listByClassId(Long classId) {
        Class classEntity = classRepository.findById(classId).orElseThrow(() -> BizException.notFound("班级不存在"));
        accessService.assertClassInScope(classEntity);
        return classCourseRepository.findByClassEntityId(classId).stream()
            .map(this::toVo)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<ClassCourseVo> listEnabledByClassId(Long classId) {
        Class classEntity = classRepository.findById(classId).orElseThrow(() -> BizException.notFound("班级不存在"));
        accessService.assertClassInScope(classEntity);
        return classCourseRepository.findByClassEntityIdAndStatus(classId, 1).stream()
            .map(this::toVo)
            .collect(Collectors.toList());
    }
    
    @Override
    public ClassCourseVo detail(Long id) {
        ClassCourse classCourse = classCourseRepository.findById(id)
            .orElseThrow(() -> BizException.notFound("班级授课不存在"));
        accessService.assertClassInScope(classCourse.getClassEntity());
        return toVo(classCourse);
    }
    
    @Override
    @Transactional
    public ClassCourseVo create(ClassCourseVo classCourseVo) {
        if (classCourseVo == null || classCourseVo.getClassId() == null) {
            throw BizException.badRequest("班级ID不能为空，请先创建班级");
        }
        if (classCourseVo.getCourseId() == null) {
            throw BizException.badRequest("科目ID不能为空，请先创建科目");
        }
        if (classCourseVo.getAcademicYear() == null || classCourseVo.getAcademicYear().isBlank()) {
            throw BizException.badRequest("学年不能为空");
        }
        Integer semester = requiredSemester(classCourseVo.getSemester());
        Class classEntity = classRepository.findById(classCourseVo.getClassId())
            .orElseThrow(() -> BizException.notFound("班级不存在，请先创建班级"));
        accessService.assertClassInScope(classEntity);
        Course course = courseRepository.findById(classCourseVo.getCourseId())
            .orElseThrow(() -> BizException.notFound("科目不存在，请先创建科目"));
        classCourseRepository.findByClassAndCourseAndSemesterAndYear(
            classCourseVo.getClassId(), classCourseVo.getCourseId(), semester, classCourseVo.getAcademicYear().trim()
        ).ifPresent(existing -> {
            throw BizException.conflict("该班级本学期已存在相同课程");
        });
        assertSessions(classCourseVo.getSessions());
        assertNoOverlap(classEntity.getId(), classCourseVo.getAcademicYear().trim(), semester, null, classCourseVo.getSessions());

        ClassCourse classCourse = new ClassCourse();
        classCourse.setClassEntity(classEntity);
        classCourse.setCourse(course);
        classCourse.setAcademicYear(classCourseVo.getAcademicYear().trim());
        classCourse.setSemester(semester);
        classCourse.setTeacher(resolveTeacher(classCourseVo.getTeacherId()));
        classCourse.setStatus(classCourseVo.getStatus() == null ? 1 : classCourseVo.getStatus());
        if (classCourse.getStatus() != 0 && classCourse.getStatus() != 1) {
            throw BizException.unprocessable("排课状态只能是 0 或 1");
        }
        classCourse = classCourseRepository.save(classCourse);
        replaceSessions(classCourse.getId(), classCourseVo.getSessions());
        auditLogService.recordEvent(9, AuthContext.userId(), null, "CLASS_COURSE", String.valueOf(classCourse.getId()),
                "create classId=" + classEntity.getId() + " courseId=" + course.getId());
        return toVo(classCourse);
    }
    
    @Override
    @Transactional
    public void batchCreate(Long classId, List<Long> courseIds, String academicYear, Integer semester) {
        if (classId == null || courseIds == null || courseIds.isEmpty()) {
            throw BizException.badRequest("班级和课程不能为空");
        }
        if (academicYear == null || academicYear.isBlank() || semester == null || (semester != 1 && semester != 2)) {
            throw BizException.badRequest("批量排课需要 academicYear，以及 semester=1 或 2");
        }
        Class classEntity = classRepository.findById(classId).orElseThrow(() -> BizException.notFound("班级不存在"));
        accessService.assertClassInScope(classEntity);
        for (Long courseId : courseIds) {
            if (classCourseRepository.findByClassAndCourseAndSemesterAndYear(classId, courseId, semester, academicYear.trim()).isPresent()) {
                continue;
            }
            Course course = courseRepository.findById(courseId).orElseThrow(() -> BizException.notFound("科目不存在：ID=" + courseId));
            ClassCourse classCourse = new ClassCourse();
            classCourse.setClassEntity(classEntity);
            classCourse.setCourse(course);
            classCourse.setAcademicYear(academicYear.trim());
            classCourse.setSemester(semester);
            classCourse.setStatus(1);
            classCourseRepository.save(classCourse);
        }
        auditLogService.recordEvent(9, AuthContext.userId(), null, "CLASS_COURSE", String.valueOf(classId), "batch-create");
    }
    
    @Override
    @Transactional
    public ClassCourseVo update(Long id, ClassCourseVo classCourseVo) {
        ClassCourse classCourse = classCourseRepository.findById(id)
            .orElseThrow(() -> BizException.notFound("班级授课不存在"));
        accessService.assertClassInScope(classCourse.getClassEntity());
        if (classCourseVo == null) {
            throw BizException.badRequest("请求体不能为空");
        }
        Long classId = classCourseVo.getClassId() == null ? classCourse.getClassEntity().getId() : classCourseVo.getClassId();
        Long courseId = classCourseVo.getCourseId() == null ? classCourse.getCourse().getId() : classCourseVo.getCourseId();
        String year = classCourseVo.getAcademicYear() == null ? classCourse.getAcademicYear() : classCourseVo.getAcademicYear().trim();
        Integer semester = classCourseVo.getSemester() == null ? classCourse.getSemester() : requiredSemester(classCourseVo.getSemester());
        Class classEntity = classRepository.findById(classId).orElseThrow(() -> BizException.notFound("班级不存在"));
        accessService.assertClassInScope(classEntity);
        Course course = courseRepository.findById(courseId).orElseThrow(() -> BizException.notFound("科目不存在"));
        classCourseRepository.findByClassAndCourseAndSemesterAndYear(classId, courseId, semester, year).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw BizException.conflict("该班级本学期已存在相同课程");
            }
        });
        if (classCourseVo.getSessions() != null) {
            assertSessions(classCourseVo.getSessions());
            assertNoOverlap(classId, year, semester, id, classCourseVo.getSessions());
        }
        classCourse.setClassEntity(classEntity);
        classCourse.setCourse(course);
        classCourse.setAcademicYear(year);
        classCourse.setSemester(semester);
        if (classCourseVo.getTeacherId() != null) {
            classCourse.setTeacher(resolveTeacher(classCourseVo.getTeacherId()));
        }
        if (classCourseVo.getStatus() != null) {
            if (classCourseVo.getStatus() != 0 && classCourseVo.getStatus() != 1) {
                throw BizException.unprocessable("排课状态只能是 0 或 1");
            }
            classCourse.setStatus(classCourseVo.getStatus());
        }
        classCourseRepository.save(classCourse);
        if (classCourseVo.getSessions() != null) {
            replaceSessions(classCourse.getId(), classCourseVo.getSessions());
        }
        auditLogService.recordEvent(9, AuthContext.userId(), null, "CLASS_COURSE", String.valueOf(id), "update");
        return toVo(classCourse);
    }
    
    @Override
    @Transactional
    public void delete(Long id) {
        ClassCourse classCourse = classCourseRepository.findById(id)
            .orElseThrow(() -> BizException.notFound("班级授课不存在"));
        accessService.assertClassInScope(classCourse.getClassEntity());
        classCourseSessionRepository.deleteByClassCourseId(id);
        classCourseRepository.delete(classCourse);
        auditLogService.recordEvent(9, AuthContext.userId(), null, "CLASS_COURSE", String.valueOf(id), "delete");
    }
    
    @Override
    @Transactional
    public void batchDelete(List<Long> ids) {
        if (ids == null) {
            throw BizException.badRequest("ids 不能为空");
        }
        for (Long id : ids) {
            if (classCourseRepository.existsById(id)) {
                delete(id);
            }
        }
    }
    
    /**
     * 转换为VO
     */
    private ClassCourseVo toVo(ClassCourse classCourse) {
        ClassCourseVo vo = new ClassCourseVo();
        BeanMapper.copyProperties(classCourse, vo);
        
        // 设置班级信息
        if (classCourse.getClassEntity() != null) {
            vo.setClassId(classCourse.getClassEntity().getId());
            vo.setClassName(classCourse.getClassEntity().getClassName());
            vo.setClassCode(classCourse.getClassEntity().getClassCode());
        }
        
        // 设置科目信息
        if (classCourse.getCourse() != null) {
            vo.setCourseId(classCourse.getCourse().getId());
            vo.setCourseCode(classCourse.getCourse().getCourseCode());
            vo.setCourseName(classCourse.getCourse().getCourseName());
            vo.setCourseType(classCourse.getCourse().getCourseType());
        }
        
        // 设置教师信息
        if (classCourse.getSemester() != null) {
            vo.setSemester(String.valueOf(classCourse.getSemester()));
        }
        if (classCourse.getTeacher() != null) {
            vo.setTeacherId(classCourse.getTeacher().getId());
            vo.setTeacherName(classCourse.getTeacher().getRealName());
        }
        vo.setSessions(classCourseSessionRepository.findByClassCourseId(classCourse.getId()).stream().map(session -> {
            SessionSlot slot = new SessionSlot();
            slot.setWeekday(session.getWeekday());
            slot.setPeriodStart(session.getPeriodStart());
            slot.setPeriodEnd(session.getPeriodEnd());
            slot.setRoomNo(session.getRoomNo());
            return slot;
        }).collect(Collectors.toList()));
        return vo;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ClassCourseVo> list(long page, long size, Long classId) {
        List<ClassCourseVo> all = (classId == null ? classCourseRepository.findAll() : classCourseRepository.findByClassEntityId(classId))
                .stream()
                .filter(row -> inScope(row.getClassEntity()))
                .map(this::toVo)
                .collect(Collectors.toList());
        int from = (int) Math.min((page - 1) * size, all.size());
        int to = (int) Math.min(from + size, all.size());
        return PageResult.<ClassCourseVo>builder().page(page).size(size).total(all.size()).records(all.subList(from, to)).build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimetableItemVo> myTimetable(Long userId) {
        AuthPrincipal principal = AuthContext.get();
        boolean dev = principal != null && principal.devAuth();
        if (userId == null && !dev) {
            throw BizException.unauthorized("未登录");
        }
        DataScope scope = accessService.current();
        String highest = dev
                ? Phase1Roles.highest(principal.roles())
                : Phase1Roles.highest(accessService.rolesOf(
                userRepository.findById(userId).orElseThrow(() -> BizException.notFound("用户不存在"))));
        List<ClassCourse> courses;
        if (Phase1Roles.STUDENT.equals(highest)) {
            Long classId = scope.getClassIds() == null || scope.getClassIds().isEmpty() ? null : scope.getClassIds().get(0);
            if (!dev) {
                Student student = studentRepository.findByUserId(userId).orElseThrow(() -> BizException.notFound("未绑定学籍"));
                classId = student.getClassId();
            }
            courses = classId == null ? List.of() : classCourseRepository.findByClassEntityId(classId);
        } else if (Phase1Roles.TEACHER.equals(highest)) {
            if (dev && scope.getClassIds() != null && !scope.getClassIds().isEmpty()) {
                courses = new ArrayList<>();
                for (Long id : scope.getClassIds()) {
                    courses.addAll(classCourseRepository.findByClassEntityId(id));
                }
            } else if (userId != null) {
                courses = classCourseRepository.findByTeacher_Id(userId);
            } else {
                courses = List.of();
            }
        } else if (scope.isAll()) {
            courses = classCourseRepository.findAll();
        } else {
            courses = new ArrayList<>();
            for (Long id : scope.getClassIds()) {
                courses.addAll(classCourseRepository.findByClassEntityId(id));
            }
        }
        List<TimetableItemVo> items = new ArrayList<>();
        for (ClassCourse course : courses) {
            ClassCourseVo vo = toVo(course);
            if (vo.getSessions() == null) {
                continue;
            }
            for (SessionSlot slot : vo.getSessions()) {
                items.add(TimetableItemVo.builder()
                        .classCourseId(vo.getId())
                        .classId(vo.getClassId())
                        .className(vo.getClassName())
                        .courseId(vo.getCourseId())
                        .courseName(vo.getCourseName())
                        .teacherId(vo.getTeacherId())
                        .teacherName(vo.getTeacherName())
                        .academicYear(vo.getAcademicYear())
                        .semester(vo.getSemester())
                        .weekday(slot.getWeekday())
                        .periodStart(slot.getPeriodStart())
                        .periodEnd(slot.getPeriodEnd())
                        .roomNo(slot.getRoomNo())
                        .build());
            }
        }
        return items;
    }

    private Integer requiredSemester(String raw) {
        Integer semester = semesterOf(raw);
        if (semester == null || (semester != 1 && semester != 2)) {
            throw BizException.unprocessable("semester 只能是 1 或 2");
        }
        return semester;
    }

    private User resolveTeacher(Long teacherId) {
        if (teacherId == null) {
            return null;
        }
        User teacher = userRepository.findById(teacherId).orElseThrow(() -> BizException.notFound("授课教师不存在"));
        if (teacher.getUserType() == null || teacher.getUserType() != 2) {
            throw BizException.unprocessable("授课教师必须是教职工账号");
        }
        return teacher;
    }

    private void assertSessions(List<SessionSlot> sessions) {
        if (sessions == null) {
            return;
        }
        for (int i = 0; i < sessions.size(); i++) {
            SessionSlot left = sessions.get(i);
            validateSlot(left);
            for (int j = i + 1; j < sessions.size(); j++) {
                SessionSlot right = sessions.get(j);
                validateSlot(right);
                if (overlaps(left, right)) {
                    throw BizException.conflict("同一授课的节次互相重叠");
                }
            }
        }
    }

    private void assertNoOverlap(Long classId, String academicYear, Integer semester, Long ignoreId, List<SessionSlot> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        for (ClassCourse existing : classCourseRepository.findByClassEntityId(classId)) {
            if (ignoreId != null && ignoreId.equals(existing.getId())) {
                continue;
            }
            if (!academicYear.equals(existing.getAcademicYear()) || !semester.equals(existing.getSemester())) {
                continue;
            }
            List<SessionSlot> others = classCourseSessionRepository.findByClassCourseId(existing.getId()).stream().map(row -> {
                SessionSlot slot = new SessionSlot();
                slot.setWeekday(row.getWeekday());
                slot.setPeriodStart(row.getPeriodStart());
                slot.setPeriodEnd(row.getPeriodEnd());
                return slot;
            }).toList();
            for (SessionSlot slot : sessions) {
                for (SessionSlot other : others) {
                    if (overlaps(slot, other)) {
                        throw BizException.conflict("节次与班内已有课程重叠");
                    }
                }
            }
        }
    }

    private void validateSlot(SessionSlot slot) {
        if (slot.getWeekday() == null || slot.getWeekday() < 1 || slot.getWeekday() > 7) {
            throw BizException.unprocessable("weekday 必须是 1-7");
        }
        if (slot.getPeriodStart() == null || slot.getPeriodEnd() == null
                || slot.getPeriodStart() < 1 || slot.getPeriodEnd() > 20 || slot.getPeriodEnd() < slot.getPeriodStart()) {
            throw BizException.unprocessable("periodStart/periodEnd 不合法");
        }
    }

    private boolean overlaps(SessionSlot left, SessionSlot right) {
        if (left.getWeekday() == null || !left.getWeekday().equals(right.getWeekday())) {
            return false;
        }
        return left.getPeriodStart() <= right.getPeriodEnd() && right.getPeriodStart() <= left.getPeriodEnd();
    }

    private boolean inScope(Class schoolClass) {
        if (schoolClass == null) {
            return false;
        }
        try {
            accessService.assertClassInScope(schoolClass);
            return true;
        } catch (BizException ex) {
            return false;
        }
    }

    private Integer semesterOf(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(raw.trim());
        } catch (NumberFormatException ex) {
            throw BizException.unprocessable("semester 只能是 1 或 2");
        }
    }

    private void replaceSessions(Long classCourseId, List<SessionSlot> sessions) {
        classCourseSessionRepository.deleteByClassCourseId(classCourseId);
        if (sessions == null) {
            return;
        }
        for (SessionSlot slot : sessions) {
            if (slot.getWeekday() == null || slot.getWeekday() < 1 || slot.getWeekday() > 7) {
                throw BizException.unprocessable("weekday 必须是 1-7");
            }
            if (slot.getPeriodStart() == null || slot.getPeriodEnd() == null || slot.getPeriodStart() < 1 || slot.getPeriodEnd() < slot.getPeriodStart()) {
                throw BizException.unprocessable("periodStart/periodEnd 不合法");
            }
            ClassCourseSession session = new ClassCourseSession();
            session.setClassCourseId(classCourseId);
            session.setWeekday(slot.getWeekday());
            session.setPeriodStart(slot.getPeriodStart());
            session.setPeriodEnd(slot.getPeriodEnd());
            session.setRoomNo(slot.getRoomNo());
            classCourseSessionRepository.save(session);
        }
    }
    
    /**
     * 安全的空值比较
     */
    private boolean equalsNullable(Object a, Object b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
    }
}

