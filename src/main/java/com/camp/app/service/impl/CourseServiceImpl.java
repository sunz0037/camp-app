package com.camp.app.service.impl;

import com.camp.app.api.BizException;
import com.camp.app.api.PageResult;
import com.camp.app.entity.Course;
import com.camp.app.model.CourseVo;
import com.camp.app.repository.ClassCourseRepository;
import com.camp.app.repository.CourseRepository;
import com.camp.app.repository.UserRepository;
import com.camp.app.security.AuthContext;
import com.camp.app.service.AuditLogService;
import com.camp.app.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {
    private static final int COURSE_WRITE = 8;
    private final CourseRepository courseRepository;
    private final ClassCourseRepository classCourseRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Override
    public PageResult<CourseVo> list(long page, long size, String semester, String academicYear, Long teacherId, String keyword) {
        Pageable pageable = PageRequest.of((int) (page - 1), (int) size);
        Page<Course> coursePage = courseRepository.findByFilters(semester, academicYear, teacherId, keyword, pageable);
        List<CourseVo> voList = coursePage.getContent().stream().map(this::toVo).toList();
        
        return PageResult.<CourseVo>builder()
                .page(page)
                .size(size)
                .total(coursePage.getTotalElements())
                .records(voList)
                .build();
    }

    @Override
    public CourseVo detail(Long id) {
        return courseRepository.findById(id).map(this::toVo).orElseThrow(() -> BizException.notFound("课程不存在"));
    }

    @Override
    @Transactional
    public CourseVo create(CourseVo vo) {
        Course course = new Course();
        apply(course, vo, true);
        course = courseRepository.save(course);
        auditLogService.recordEvent(COURSE_WRITE, AuthContext.userId(), null, "COURSE", String.valueOf(course.getId()),
                "create " + course.getCourseCode());
        return toVo(course);
    }

    @Override
    @Transactional
    public CourseVo update(Long id, CourseVo vo) {
        Course course = courseRepository.findById(id).orElseThrow(() -> BizException.notFound("课程不存在"));
        apply(course, vo, false);
        course.setId(id);
        courseRepository.save(course);
        auditLogService.recordEvent(COURSE_WRITE, AuthContext.userId(), null, "COURSE", String.valueOf(id), "update");
        return toVo(course);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!courseRepository.existsById(id)) {
            throw BizException.notFound("课程不存在");
        }
        if (!classCourseRepository.findByCourseId(id).isEmpty()) {
            throw BizException.conflict("课程已被排课引用，不能删除");
        }
        courseRepository.deleteById(id);
        auditLogService.recordEvent(COURSE_WRITE, AuthContext.userId(), null, "COURSE", String.valueOf(id), "delete");
    }

    private void apply(Course course, CourseVo vo, boolean creating) {
        if (vo == null) {
            throw BizException.badRequest("请求体不能为空");
        }
        if (creating || StringUtils.hasText(vo.getCourseCode())) {
            if (!StringUtils.hasText(vo.getCourseCode())) {
                throw BizException.badRequest("课程代码不能为空");
            }
            String code = vo.getCourseCode().trim();
            if (code.length() > 20) {
                throw BizException.unprocessable("课程代码不能超过 20 位");
            }
            courseRepository.findByCourseCode(code).ifPresent(existing -> {
                if (course.getId() == null || !existing.getId().equals(course.getId())) {
                    throw BizException.conflict("课程代码已存在");
                }
            });
            course.setCourseCode(code);
        }
        if (creating || StringUtils.hasText(vo.getCourseName())) {
            if (!StringUtils.hasText(vo.getCourseName())) {
                throw BizException.badRequest("课程名称不能为空");
            }
            if (vo.getCourseName().trim().length() > 100) {
                throw BizException.unprocessable("课程名称不能超过 100 字");
            }
            course.setCourseName(vo.getCourseName().trim());
        }
        if (creating || vo.getCredit() != null) {
            if (vo.getCredit() == null) {
                throw BizException.badRequest("学分不能为空");
            }
            if (vo.getCredit() < 0 || vo.getCredit() > 99) {
                throw BizException.unprocessable("学分需在 0 到 99 之间");
            }
            course.setCredit(BigDecimal.valueOf(vo.getCredit()));
        }
        if (vo.getCourseType() != null) {
            course.setCourseType(vo.getCourseType());
        }
        if (vo.getDepartment() != null) {
            course.setDepartment(vo.getDepartment());
        }
        if (vo.getDescription() != null) {
            course.setDescription(vo.getDescription());
        }
        if (vo.getSemester() != null) {
            course.setSemester(vo.getSemester());
        }
        if (vo.getAcademicYear() != null) {
            course.setAcademicYear(vo.getAcademicYear());
        }
        if (vo.getStatus() != null) {
            if (vo.getStatus() != 0 && vo.getStatus() != 1) {
                throw BizException.unprocessable("课程状态只能是 0 或 1");
            }
            course.setStatus(vo.getStatus());
        } else if (creating) {
            course.setStatus(1);
        }
        if (vo.getTeacherId() != null) {
            var teacher = userRepository.findById(vo.getTeacherId()).orElseThrow(() -> BizException.notFound("默认教师不存在"));
            if (teacher.getUserType() == null || teacher.getUserType() != 2) {
                throw BizException.unprocessable("默认教师必须是教职工账号");
            }
            course.setTeacherId(vo.getTeacherId());
        }
    }
    
    @Override
    public List<CourseVo> getAllEnabledCourses() {
        return courseRepository.findAll().stream()
            .filter(course -> course.getStatus() != null && course.getStatus() == 1)
            .map(this::toVo)
            .toList();
    }

    private CourseVo toVo(Course course) {
        CourseVo vo = new CourseVo();
        vo.setId(course.getId());
        vo.setCourseCode(course.getCourseCode());
        vo.setCourseName(course.getCourseName());
        vo.setCredit(course.getCredit() == null ? null : course.getCredit().doubleValue());
        vo.setCourseType(course.getCourseType());
        vo.setDepartment(course.getDepartment());
        vo.setTeacherId(course.getTeacherId());
        vo.setSemester(course.getSemester());
        vo.setAcademicYear(course.getAcademicYear());
        vo.setDescription(course.getDescription());
        vo.setStatus(course.getStatus());
        vo.setCreatedAt(course.getCreatedAt());
        vo.setUpdatedAt(course.getUpdatedAt());
        return vo;
    }
}

