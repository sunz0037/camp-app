package com.camp.app.service;

import com.camp.app.api.BizException;
import com.camp.app.entity.Class;
import com.camp.app.entity.Course;
import com.camp.app.model.ExamScheduleVo;
import com.camp.app.repository.ClassRepository;
import com.camp.app.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 把前端考试安排请求补成 edu_exams 能存的字段。不写第二套表。
 * 旧路径 /exam-schedules 不经过这里，缺学年学期仍然返回 400。
 */
@Component
@RequiredArgsConstructor
public class ExamAliasAdapter {
    private final CourseRepository courseRepository;
    private final ClassRepository classRepository;

    @Transactional(readOnly = true)
    public void prepareCreate(ExamScheduleVo vo) {
        if (vo == null) {
            return;
        }
        vo.setCourseId(resolveCourse(vo.getCourseId(), vo.getCourseName(), true));
        vo.setClassId(resolveClass(vo.getClassId(), vo.getClassName(), vo.getGradeName(), true));
        fillTerm(vo);
    }

    @Transactional(readOnly = true)
    public void prepareUpdate(ExamScheduleVo vo) {
        if (vo == null) {
            return;
        }
        if (hasText(vo.getCourseName())) {
            vo.setCourseId(resolveCourse(vo.getCourseId(), vo.getCourseName(), true));
        }
        if (hasText(vo.getClassName())) {
            vo.setClassId(resolveClass(vo.getClassId(), vo.getClassName(), vo.getGradeName(), true));
        }
    }

    private Long resolveCourse(Long courseId, String courseName, boolean required) {
        String name = blank(courseName);
        if (name != null) {
            List<Course> matched = courseRepository.findByCourseName(name);
            if (matched.isEmpty()) {
                throw BizException.badRequest("课程不存在");
            }
            if (matched.size() > 1) {
                throw BizException.badRequest("课程名称不唯一，请传 courseId");
            }
            return matched.get(0).getId();
        }
        if (courseId != null) {
            return courseId;
        }
        if (required) {
            throw BizException.badRequest("课程不能为空，请传 courseId，或与课程名称完全一致的 subject");
        }
        return null;
    }

    private Long resolveClass(Long classId, String className, String gradeName, boolean required) {
        String name = blank(className);
        if (name != null) {
            String grade = blank(gradeName);
            List<Class> matched = classRepository.findAll().stream()
                    .filter(row -> name.equals(row.getClassName()))
                    .filter(row -> grade == null || grade.equals(gradeNameOf(row)))
                    .toList();
            if (matched.isEmpty()) {
                throw BizException.badRequest("班级不存在");
            }
            if (matched.size() > 1) {
                throw BizException.badRequest("班级名称不唯一，请传 classId");
            }
            return matched.get(0).getId();
        }
        if (classId != null) {
            return classId;
        }
        if (required) {
            throw BizException.badRequest("班级不能为空，请传 classId，或与班级名称完全一致的 className");
        }
        return null;
    }

    /**
     * 只补缺的学年或学期。9 月到次年 1 月为第一学期，2 月到 8 月为第二学期。
     */
    private void fillTerm(ExamScheduleVo vo) {
        boolean yearBlank = !hasText(vo.getAcademicYear());
        if (!yearBlank && vo.getSemester() != null) {
            return;
        }
        if (vo.getExamDate() == null) {
            return;
        }
        LocalDate date = vo.getExamDate().toLocalDate();
        int month = date.getMonthValue();
        int year = date.getYear();
        int startYear = month >= 9 ? year : year - 1;
        if (vo.getSemester() == null) {
            vo.setSemester(month >= 9 || month == 1 ? 1 : 2);
        }
        if (yearBlank) {
            vo.setAcademicYear(startYear + "-" + (startYear + 1));
        }
    }

    private String gradeNameOf(Class schoolClass) {
        if (schoolClass == null || schoolClass.getGrade() == null) {
            return null;
        }
        return schoolClass.getGrade().getGradeName();
    }

    private boolean hasText(String raw) {
        return blank(raw) != null;
    }

    private String blank(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        return value.isEmpty() ? null : value;
    }
}
