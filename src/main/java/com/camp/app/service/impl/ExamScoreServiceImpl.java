package com.camp.app.service.impl;

import com.camp.app.api.BizException;
import com.camp.app.api.PageResult;
import com.camp.app.entity.ExamScore;
import com.camp.app.entity.Student;
import com.camp.app.model.DataScope;
import com.camp.app.model.ExamScoreVo;
import com.camp.app.repository.CourseRepository;
import com.camp.app.repository.ExamScoreRepository;
import com.camp.app.repository.StudentRepository;
import com.camp.app.security.AuthContext;
import com.camp.app.service.AccessService;
import com.camp.app.service.ExamScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamScoreServiceImpl implements ExamScoreService {
    private final ExamScoreRepository examScoreRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final AccessService accessService;

    @Override
    @Transactional(readOnly = true)
    public PageResult<ExamScoreVo> list(long page, long size, Long studentId, Long courseId, Long examScheduleId, String semester, String academicYear, String examType) {
        DataScope scope = accessService.current();
        if ("SELF".equals(scope.getLevel())) {
            if (scope.getStudentId() == null) {
                return PageResult.<ExamScoreVo>builder().page(page).size(size).total(0).records(List.of()).build();
            }
            if (studentId != null && !studentId.equals(scope.getStudentId())) {
                throw BizException.forbidden("只能查看本人成绩");
            }
            studentId = scope.getStudentId();
        }
        List<ExamScoreVo> all = examScoreRepository.findByFilters(
                        studentId, courseId, examScheduleId, semester, academicYear, examType, PageRequest.of(0, 500))
                .getContent().stream()
                .filter(row -> visible(scope, row.getStudentId()))
                .map(this::toVo)
                .toList();
        int from = (int) Math.min((page - 1) * size, all.size());
        int to = (int) Math.min(from + size, all.size());
        return PageResult.<ExamScoreVo>builder().page(page).size(size).total(all.size()).records(all.subList(from, to)).build();
    }

    @Override
    public ExamScoreVo detail(Long id) {
        ExamScore score = examScoreRepository.findById(id).orElseThrow(() -> BizException.notFound("成绩不存在"));
        accessService.assertStudentVisible(score.getStudentId());
        return toVo(score);
    }

    @Override
    @Transactional
    public ExamScoreVo create(ExamScoreVo vo) {
        ExamScore score = new ExamScore();
        apply(score, vo, true);
        score.setCreatedBy(AuthContext.userId());
        score = examScoreRepository.save(score);
        return toVo(score);
    }

    @Override
    @Transactional
    public ExamScoreVo update(Long id, ExamScoreVo vo) {
        ExamScore score = examScoreRepository.findById(id).orElseThrow(() -> BizException.notFound("成绩不存在"));
        accessService.assertStudentVisible(score.getStudentId());
        apply(score, vo, false);
        score.setId(id);
        examScoreRepository.save(score);
        return toVo(score);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ExamScore score = examScoreRepository.findById(id).orElseThrow(() -> BizException.notFound("成绩不存在"));
        accessService.assertStudentVisible(score.getStudentId());
        examScoreRepository.delete(score);
    }

    private void apply(ExamScore score, ExamScoreVo vo, boolean creating) {
        if (vo == null) {
            throw BizException.badRequest("请求体不能为空");
        }
        Long studentId = vo.getStudentId() != null ? vo.getStudentId() : score.getStudentId();
        Long courseId = vo.getCourseId() != null ? vo.getCourseId() : score.getCourseId();
        String examType = vo.getExamType() != null ? vo.getExamType().trim() : score.getExamType();
        String semester = vo.getSemester() != null ? vo.getSemester().trim() : score.getSemester();
        String academicYear = vo.getAcademicYear() != null ? vo.getAcademicYear().trim() : score.getAcademicYear();
        if (creating && (studentId == null || courseId == null || examType == null || examType.isBlank()
                || semester == null || semester.isBlank() || academicYear == null || academicYear.isBlank())) {
            throw BizException.badRequest("成绩需要 studentId、courseId、examType、semester、academicYear");
        }
        if (studentId != null) {
            Student student = studentRepository.findById(studentId).orElseThrow(() -> BizException.notFound("学生不存在"));
            accessService.assertStudentVisible(student.getId());
            score.setStudentId(studentId);
        }
        if (courseId != null) {
            if (!courseRepository.existsById(courseId)) {
                throw BizException.notFound("课程不存在");
            }
            score.setCourseId(courseId);
        }
        if (examType != null) {
            if (examType.length() > 20) {
                throw BizException.unprocessable("examType 过长");
            }
            score.setExamType(examType);
        }
        if (semester != null) {
            score.setSemester(semester);
        }
        if (academicYear != null) {
            score.setAcademicYear(academicYear);
        }
        if (vo.getScore() != null) {
            if (vo.getScore() < 0 || vo.getScore() > 100) {
                throw BizException.unprocessable("分数需在 0 到 100 之间");
            }
            score.setScore(BigDecimal.valueOf(vo.getScore()).setScale(2, RoundingMode.HALF_UP));
            if (vo.getGradePoint() == null) {
                score.setGradePoint(gradePoint(vo.getScore()));
            }
        }
        if (vo.getGradePoint() != null) {
            score.setGradePoint(BigDecimal.valueOf(vo.getGradePoint()).setScale(2, RoundingMode.HALF_UP));
        }
        if (vo.getCredit() != null) {
            score.setCredit(BigDecimal.valueOf(vo.getCredit()).setScale(1, RoundingMode.HALF_UP));
        }
        if (vo.getExamDate() != null) {
            score.setExamDate(vo.getExamDate());
        }
        if (vo.getExamScheduleId() != null) {
            score.setExamScheduleId(vo.getExamScheduleId());
        }
        if (vo.getRemark() != null) {
            score.setRemark(vo.getRemark());
        }
        if (vo.getRemarkDetail() != null) {
            score.setRemarkDetail(vo.getRemarkDetail());
        }
        if (creating) {
            examScoreRepository.findByStudentIdAndCourseIdAndExamTypeAndSemesterAndAcademicYear(
                    score.getStudentId(), score.getCourseId(), score.getExamType(), score.getSemester(), score.getAcademicYear()
            ).ifPresent(existing -> {
                throw BizException.conflict("该学生本学期这门考试成绩已存在");
            });
        }
    }

    private BigDecimal gradePoint(double score) {
        double point = score >= 90 ? 4.0 : score >= 80 ? 3.0 : score >= 70 ? 2.0 : score >= 60 ? 1.0 : 0;
        return BigDecimal.valueOf(point).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean visible(DataScope scope, Long studentId) {
        if (scope.isAll()) {
            return true;
        }
        try {
            accessService.assertStudentVisible(studentId);
            return true;
        } catch (BizException ex) {
            return false;
        }
    }

    private ExamScoreVo toVo(ExamScore score) {
        ExamScoreVo vo = new ExamScoreVo();
        vo.setId(score.getId());
        vo.setStudentId(score.getStudentId());
        vo.setCourseId(score.getCourseId());
        vo.setExamScheduleId(score.getExamScheduleId());
        vo.setExamType(score.getExamType());
        vo.setScore(score.getScore() == null ? null : score.getScore().doubleValue());
        vo.setCredit(score.getCredit() == null ? null : score.getCredit().doubleValue());
        vo.setGradePoint(score.getGradePoint() == null ? null : score.getGradePoint().doubleValue());
        vo.setExamDate(score.getExamDate());
        vo.setSemester(score.getSemester());
        vo.setAcademicYear(score.getAcademicYear());
        vo.setRemark(score.getRemark());
        vo.setRemarkDetail(score.getRemarkDetail());
        vo.setCreatedBy(score.getCreatedBy());
        vo.setCreatedAt(score.getCreatedAt());
        vo.setUpdatedAt(score.getUpdatedAt());
        return vo;
    }
}
