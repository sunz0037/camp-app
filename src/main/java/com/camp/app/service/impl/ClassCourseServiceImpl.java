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
    
    @Override
    public List<ClassCourseVo> listByClassId(Long classId) {
        return classCourseRepository.findByClassEntityId(classId).stream()
            .map(this::toVo)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<ClassCourseVo> listEnabledByClassId(Long classId) {
        return classCourseRepository.findByClassEntityIdAndStatus(classId, 1).stream()
            .map(this::toVo)
            .collect(Collectors.toList());
    }
    
    @Override
    public ClassCourseVo detail(Long id) {
        ClassCourse classCourse = classCourseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("班级授课科目关联不存在"));
        return toVo(classCourse);
    }
    
    @Override
    @Transactional
    public void create(ClassCourseVo classCourseVo) {
        // 验证维护顺序：必须存在班级（优先级2）
        if (classCourseVo.getClassId() == null) {
            throw new RuntimeException("班级ID不能为空，请先创建班级");
        }
        Class classEntity = classRepository.findById(classCourseVo.getClassId())
            .orElseThrow(() -> new RuntimeException("班级不存在，请先创建班级"));
        
        // 验证维护顺序：必须存在科目（优先级3）
        if (classCourseVo.getCourseId() == null) {
            throw new RuntimeException("科目ID不能为空，请先创建科目");
        }
        Course course = courseRepository.findById(classCourseVo.getCourseId())
            .orElseThrow(() -> new RuntimeException("科目不存在，请先创建科目"));
        
        // 检查是否已存在相同的关联（同一班级、同一科目、同一学期、同一学年）
        classCourseRepository.findByClassAndCourseAndSemesterAndYear(
            classCourseVo.getClassId(),
            classCourseVo.getCourseId(),
            classCourseVo.getSemester(),
            classCourseVo.getAcademicYear()
        ).ifPresent(existing -> {
            throw new RuntimeException("该班级已存在相同的授课科目关联");
        });
        
        ClassCourse classCourse = new ClassCourse();
        BeanMapper.copyProperties(classCourseVo, classCourse);
        classCourse.setClassEntity(classEntity);
        classCourse.setCourse(course);
        
        // 设置授课教师（如果提供）- 优先级4：授课科目对应授课教师
        if (classCourseVo.getTeacherId() != null) {
            User teacher = userRepository.findById(classCourseVo.getTeacherId())
                .orElseThrow(() -> new RuntimeException("授课教师不存在"));
            
            // 验证用户类型是否为教师（user_type = 2）
            if (teacher.getUserType() == null || teacher.getUserType() != 2) {
                throw new RuntimeException("指定的用户不是教师（user_type应为2）");
            }
            
            classCourse.setTeacher(teacher);
        }
        
        if (classCourse.getStatus() == null) {
            classCourse.setStatus(1);
        }
        
        classCourseRepository.save(classCourse);
        replaceSessions(classCourse.getId(), classCourseVo.getSessions());
    }
    
    @Override
    @Transactional
    public void batchCreate(Long classId, List<Long> courseIds) {
        // 验证班级是否存在
        Class classEntity = classRepository.findById(classId)
            .orElseThrow(() -> new RuntimeException("班级不存在"));
        
        for (Long courseId : courseIds) {
            // 检查是否已存在
            if (classCourseRepository.findByClassEntityIdAndCourseId(classId, courseId).isPresent()) {
                continue; // 已存在则跳过
            }
            
            Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("科目不存在：ID=" + courseId));
            
            ClassCourse classCourse = new ClassCourse();
            classCourse.setClassEntity(classEntity);
            classCourse.setCourse(course);
            classCourse.setStatus(1);
            
            classCourseRepository.save(classCourse);
        }
    }
    
    @Override
    @Transactional
    public void update(Long id, ClassCourseVo classCourseVo) {
        ClassCourse classCourse = classCourseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("班级授课科目关联不存在"));
        
        // 如果修改了班级，验证班级是否存在
        if (!classCourse.getClassEntity().getId().equals(classCourseVo.getClassId())) {
            Class classEntity = classRepository.findById(classCourseVo.getClassId())
                .orElseThrow(() -> new RuntimeException("班级不存在"));
            classCourse.setClassEntity(classEntity);
        }
        
        // 如果修改了科目，验证科目是否存在
        if (!classCourse.getCourse().getId().equals(classCourseVo.getCourseId())) {
            Course course = courseRepository.findById(classCourseVo.getCourseId())
                .orElseThrow(() -> new RuntimeException("科目不存在"));
            classCourse.setCourse(course);
        }
        
        // 检查唯一性（如果修改了关键字段）
        if (!classCourse.getClassEntity().getId().equals(classCourseVo.getClassId()) ||
            !classCourse.getCourse().getId().equals(classCourseVo.getCourseId()) ||
            !equalsNullable(classCourse.getSemester(), classCourseVo.getSemester()) ||
            !equalsNullable(classCourse.getAcademicYear(), classCourseVo.getAcademicYear())) {
            
            classCourseRepository.findByClassAndCourseAndSemesterAndYear(
                classCourseVo.getClassId(),
                classCourseVo.getCourseId(),
                classCourseVo.getSemester(),
                classCourseVo.getAcademicYear()
            ).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new RuntimeException("该班级已存在相同的授课科目关联");
                }
            });
        }
        
        // 更新其他字段
        classCourse.setSemester(classCourseVo.getSemester());
        classCourse.setAcademicYear(classCourseVo.getAcademicYear());
        classCourse.setStatus(classCourseVo.getStatus());
        
        // 更新授课教师
        if (classCourseVo.getTeacherId() != null) {
            User teacher = userRepository.findById(classCourseVo.getTeacherId())
                .orElseThrow(() -> new RuntimeException("授课教师不存在"));
            classCourse.setTeacher(teacher);
        } else {
            classCourse.setTeacher(null);
        }
        
        classCourseRepository.save(classCourse);
        if (classCourseVo.getSessions() != null) {
            replaceSessions(classCourse.getId(), classCourseVo.getSessions());
        }
    }
    
    @Override
    @Transactional
    public void delete(Long id) {
        ClassCourse classCourse = classCourseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("班级授课科目关联不存在"));
        classCourseSessionRepository.deleteByClassCourseId(id);
        classCourseRepository.delete(classCourse);
    }
    
    @Override
    @Transactional
    public void batchDelete(List<Long> ids) {
        for (Long id : ids) {
            if (classCourseRepository.existsById(id)) {
                classCourseRepository.deleteById(id);
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
                .stream().map(this::toVo).collect(Collectors.toList());
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

