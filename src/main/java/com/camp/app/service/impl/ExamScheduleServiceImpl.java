package com.camp.app.service.impl;

import com.camp.app.api.BizException;
import com.camp.app.api.PageResult;
import com.camp.app.entity.Class;
import com.camp.app.entity.Course;
import com.camp.app.entity.ExamSchedule;
import com.camp.app.entity.User;
import com.camp.app.model.DataScope;
import com.camp.app.model.ExamScheduleVo;
import com.camp.app.repository.ClassRepository;
import com.camp.app.repository.CourseRepository;
import com.camp.app.repository.ExamScheduleRepository;
import com.camp.app.repository.ExamScoreRepository;
import com.camp.app.repository.UserRepository;
import com.camp.app.service.AccessService;
import com.camp.app.service.ExamScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 考试安排落在 edu_exams。没有考试类型、公开标记和取消状态这几列。
 * /api/v1/exams 与本服务是同一张表，不另写一份。
 */
@Service
@RequiredArgsConstructor
public class ExamScheduleServiceImpl implements ExamScheduleService {
    private final ExamScheduleRepository examScheduleRepository;
    private final ExamScoreRepository examScoreRepository;
    private final ClassRepository classRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final AccessService accessService;

    @Override
    @Transactional(readOnly = true)
    public PageResult<ExamScheduleVo> list(Integer page, Integer size, String targetGrade,
                                           String examType, Boolean isPublic, Integer status,
                                           String keyword) {
        return list(page, size, targetGrade, examType, isPublic, status, keyword, null, null, null, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ExamScheduleVo> list(Integer page, Integer size, String targetGrade,
                                           String examType, Boolean isPublic, Integer status,
                                           String keyword, String gradeName, Long classId, Long courseId,
                                           String academicYear, Integer semester) {
        rejectUnsupportedFilter(targetGrade, examType, isPublic, status);
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        DataScope scope = accessService.current();
        String key = blank(keyword);
        String grade = blank(gradeName);
        String year = blank(academicYear);
        Map<Long, Course> courses = courseRepository.findAll().stream()
                .collect(Collectors.toMap(Course::getId, Function.identity()));
        Map<Long, Class> classes = classRepository.findAll().stream()
                .collect(Collectors.toMap(Class::getId, Function.identity()));
        List<ExamSchedule> visible = examScheduleRepository.findAll().stream()
                .filter(row -> classVisible(scope, row.getClassId()))
                .filter(row -> classId == null || classId.equals(row.getClassId()))
                .filter(row -> courseId == null || courseId.equals(row.getCourseId()))
                .filter(row -> year == null || year.equals(row.getAcademicYear()))
                .filter(row -> semester == null || semester.equals(row.getSemester()))
                .filter(row -> grade == null || grade.equals(gradeNameOf(classes.get(row.getClassId()))))
                .filter(row -> key == null || matchesKeyword(row, key, courses, classes))
                .sorted(Comparator.comparing(ExamSchedule::getExamDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ExamSchedule::getId, Comparator.reverseOrder()))
                .toList();
        int from = Math.min((pageNo - 1) * pageSize, visible.size());
        int to = Math.min(from + pageSize, visible.size());
        List<ExamScheduleVo> items = visible.subList(from, to).stream()
                .map(row -> toVo(row, courses.get(row.getCourseId()), classes.get(row.getClassId())))
                .toList();
        return PageResult.<ExamScheduleVo>builder().page(pageNo).size(pageSize).total(visible.size()).records(items).build();
    }

    @Override
    @Transactional(readOnly = true)
    public ExamScheduleVo getById(Long id) {
        return toVo(requiredVisible(id));
    }

    @Override
    @Transactional
    public ExamScheduleVo create(ExamScheduleVo vo, Long createdBy) {
        if (vo == null) {
            throw BizException.badRequest("请求体不能为空");
        }
        if (vo.requestsCancel()) {
            throw BizException.unprocessable("edu_exams 没有状态列，取消考试未实现");
        }
        if (createdBy == null) {
            throw BizException.unauthorized("未登录");
        }
        User creator = userRepository.findById(createdBy)
                .orElseThrow(() -> BizException.notFound("创建人不存在"));
        Class schoolClass = requiredClass(vo.getClassId());
        accessService.assertClassInScope(schoolClass);
        if (vo.getCourseId() == null || !courseRepository.existsById(vo.getCourseId())) {
            throw BizException.badRequest("课程不存在");
        }
        String title = requiredText(vo.getExamName(), "考试名称", 100);
        LocalDate examDate = requiredDate(vo.getExamDate());
        String academicYear = requiredText(vo.getAcademicYear(), "学年", 20);
        int semester = requiredSemester(vo.getSemester());
        examScheduleRepository.findByClassIdAndCourseIdAndExamDateAndTitle(
                schoolClass.getId(), vo.getCourseId(), examDate, title
        ).ifPresent(existing -> {
            throw BizException.conflict("同一班级、课程、日期和名称的考试已存在");
        });
        ExamSchedule row = new ExamSchedule();
        row.setClassId(schoolClass.getId());
        row.setCourseId(vo.getCourseId());
        row.setTitle(title);
        row.setExamDate(examDate);
        row.setAcademicYear(academicYear);
        row.setSemester(semester);
        row.setCreatedBy(creator.getId());
        return toVo(examScheduleRepository.save(row));
    }

    @Override
    @Transactional
    public ExamScheduleVo update(Long id, ExamScheduleVo vo) {
        if (vo == null) {
            throw BizException.badRequest("请求体不能为空");
        }
        if (vo.requestsCancel()) {
            throw BizException.unprocessable("edu_exams 没有状态列，取消考试未实现");
        }
        ExamSchedule row = requiredVisible(id);
        if (vo.getClassId() != null) {
            Class schoolClass = requiredClass(vo.getClassId());
            accessService.assertClassInScope(schoolClass);
            row.setClassId(schoolClass.getId());
        } else {
            accessService.assertClassInScope(requiredClass(row.getClassId()));
        }
        if (vo.getCourseId() != null) {
            if (!courseRepository.existsById(vo.getCourseId())) {
                throw BizException.badRequest("课程不存在");
            }
            row.setCourseId(vo.getCourseId());
        }
        if (vo.getExamName() != null) {
            row.setTitle(requiredText(vo.getExamName(), "考试名称", 100));
        }
        if (vo.getExamDate() != null) {
            row.setExamDate(requiredDate(vo.getExamDate()));
        }
        if (vo.getAcademicYear() != null) {
            row.setAcademicYear(requiredText(vo.getAcademicYear(), "学年", 20));
        }
        if (vo.getSemester() != null) {
            row.setSemester(requiredSemester(vo.getSemester()));
        }
        examScheduleRepository.findByClassIdAndCourseIdAndExamDateAndTitle(
                row.getClassId(), row.getCourseId(), row.getExamDate(), row.getTitle()
        ).filter(existing -> !existing.getId().equals(row.getId())).ifPresent(existing -> {
            throw BizException.conflict("同一班级、课程、日期和名称的考试已存在");
        });
        return toVo(examScheduleRepository.save(row));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ExamSchedule row = requiredVisible(id);
        accessService.assertClassInScope(requiredClass(row.getClassId()));
        if (examScoreRepository.countByExamId(id) > 0) {
            throw BizException.conflict("已有成绩，不能删除考试");
        }
        examScheduleRepository.delete(row);
    }

    @Override
    public ExamScheduleVo cancel(Long id) {
        throw BizException.unprocessable("edu_exams 没有状态列，取消考试未实现");
    }

    private ExamSchedule requiredVisible(Long id) {
        ExamSchedule row = examScheduleRepository.findById(id)
                .orElseThrow(() -> BizException.notFound("考试安排不存在"));
        if (!classVisible(accessService.current(), row.getClassId())) {
            throw BizException.forbidden("超出数据范围");
        }
        return row;
    }

    private boolean classVisible(DataScope scope, Long classId) {
        if (scope == null || scope.isAll()) {
            return true;
        }
        return classId != null && scope.getClassIds() != null && scope.getClassIds().contains(classId);
    }

    private Class requiredClass(Long classId) {
        if (classId == null) {
            throw BizException.badRequest("班级不能为空");
        }
        return classRepository.findById(classId).orElseThrow(() -> BizException.badRequest("班级不存在"));
    }

    private void rejectUnsupportedFilter(String targetGrade, String examType, Boolean isPublic, Integer status) {
        if ((targetGrade != null && !targetGrade.isBlank()) || (examType != null && !examType.isBlank())
                || isPublic != null || status != null) {
            throw BizException.badRequest(ExamScheduleService.UNSUPPORTED_FILTER);
        }
    }

    private boolean matchesKeyword(ExamSchedule row, String key, Map<Long, Course> courses, Map<Long, Class> classes) {
        if (row.getTitle() != null && row.getTitle().contains(key)) {
            return true;
        }
        Course course = courses.get(row.getCourseId());
        if (course != null && course.getCourseName() != null && course.getCourseName().contains(key)) {
            return true;
        }
        Class schoolClass = classes.get(row.getClassId());
        return schoolClass != null && schoolClass.getClassName() != null && schoolClass.getClassName().contains(key);
    }

    private String gradeNameOf(Class schoolClass) {
        if (schoolClass == null || schoolClass.getGrade() == null) {
            return null;
        }
        return schoolClass.getGrade().getGradeName();
    }

    private ExamScheduleVo toVo(ExamSchedule row) {
        return toVo(row,
                row.getCourseId() == null ? null : courseRepository.findById(row.getCourseId()).orElse(null),
                row.getClassId() == null ? null : classRepository.findById(row.getClassId()).orElse(null));
    }

    private ExamScheduleVo toVo(ExamSchedule row, Course course, Class schoolClass) {
        ExamScheduleVo vo = new ExamScheduleVo();
        vo.setId(row.getId());
        vo.setExamName(row.getTitle());
        vo.setClassId(row.getClassId());
        vo.setCourseId(row.getCourseId());
        vo.setAcademicYear(row.getAcademicYear());
        vo.setSemester(row.getSemester());
        vo.setExamDate(row.getExamDate() == null ? null : row.getExamDate().atStartOfDay());
        vo.setCreatedBy(row.getCreatedBy());
        vo.setCreatedAt(row.getCreatedAt());
        vo.setUpdatedAt(row.getUpdatedAt());
        if (course != null) {
            vo.setCourseName(course.getCourseName());
        }
        if (schoolClass != null) {
            vo.setClassName(schoolClass.getClassName());
            String gradeName = gradeNameOf(schoolClass);
            vo.setGradeName(gradeName);
            vo.setTargetGrade(gradeName);
        }
        if (row.getCreatedBy() != null) {
            userRepository.findById(row.getCreatedBy()).ifPresent(user -> vo.setCreatedByName(user.getRealName()));
        }
        return vo;
    }

    private String blank(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        return value.isEmpty() ? null : value;
    }

    private String requiredText(String raw, String label, int max) {
        if (raw == null || raw.isBlank()) {
            throw BizException.badRequest(label + "不能为空");
        }
        String value = raw.trim();
        if (value.length() > max) {
            throw BizException.badRequest(label + "过长");
        }
        return value;
    }

    private LocalDate requiredDate(LocalDateTime examDate) {
        if (examDate == null) {
            throw BizException.badRequest("考试时间不能为空");
        }
        return examDate.toLocalDate();
    }

    private int requiredSemester(Integer semester) {
        if (semester == null || (semester != 1 && semester != 2)) {
            throw BizException.badRequest("学期只能是 1 或 2");
        }
        return semester;
    }
}
