package com.camp.app.service.impl;

import com.camp.app.api.BizException;
import com.camp.app.api.PageResult;
import com.camp.app.entity.ExamSchedule;
import com.camp.app.entity.ExamScore;
import com.camp.app.entity.Student;
import com.camp.app.model.DataScope;
import com.camp.app.model.ExamScoreVo;
import com.camp.app.repository.ExamScheduleRepository;
import com.camp.app.repository.ExamScoreRepository;
import com.camp.app.repository.StudentRepository;
import com.camp.app.service.AccessService;
import com.camp.app.service.ExamScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 成绩落在 edu_exam_scores。单科绩点按分数换算后放进响应，表上没有 grade_point。
 */
@Service
@RequiredArgsConstructor
public class ExamScoreServiceImpl implements ExamScoreService {
    private final ExamScoreRepository examScoreRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final StudentRepository studentRepository;
    private final AccessService accessService;

    @Override
    @Transactional(readOnly = true)
    public PageResult<ExamScoreVo> list(long page, long size, Long studentId, Long courseId, Long examScheduleId,
                                        String semester, String academicYear, String examType) {
        if (examType != null && !examType.isBlank()) {
            throw BizException.badRequest("edu_exam_scores 不支持按考试类型筛选");
        }
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
        Map<Long, ExamSchedule> exams = examScheduleRepository.findAll().stream()
                .collect(Collectors.toMap(ExamSchedule::getId, Function.identity()));
        Long filterStudent = studentId;
        List<ExamScoreVo> all = examScoreRepository.findAll().stream()
                .filter(row -> filterStudent == null || filterStudent.equals(row.getStudentId()))
                .filter(row -> examScheduleId == null || examScheduleId.equals(row.getExamId()))
                .filter(row -> visible(scope, row.getStudentId()))
                .filter(row -> matchesExam(exams.get(row.getExamId()), courseId, semester, academicYear))
                .sorted(Comparator.comparing(ExamScore::getId, Comparator.reverseOrder()))
                .map(row -> toVo(row, exams.get(row.getExamId())))
                .toList();
        int from = (int) Math.min((page - 1) * size, all.size());
        int to = (int) Math.min(from + size, all.size());
        return PageResult.<ExamScoreVo>builder().page(page).size(size).total(all.size()).records(all.subList(from, to)).build();
    }

    @Override
    @Transactional(readOnly = true)
    public ExamScoreVo detail(Long id) {
        ExamScore score = examScoreRepository.findById(id).orElseThrow(() -> BizException.notFound("成绩不存在"));
        accessService.assertStudentVisible(score.getStudentId());
        ExamSchedule exam = examScheduleRepository.findById(score.getExamId())
                .orElseThrow(() -> BizException.notFound("考试安排不存在"));
        return toVo(score, exam);
    }

    @Override
    @Transactional
    public ExamScoreVo create(ExamScoreVo vo) {
        if (vo == null || vo.getExamScheduleId() == null || vo.getStudentId() == null || vo.getScore() == null) {
            throw BizException.badRequest("成绩需要 examScheduleId、studentId、score");
        }
        ExamSchedule exam = examScheduleRepository.findById(vo.getExamScheduleId())
                .orElseThrow(() -> BizException.notFound("考试安排不存在"));
        Student student = studentRepository.findById(vo.getStudentId())
                .orElseThrow(() -> BizException.notFound("学生不存在"));
        accessService.assertStudentVisible(student.getId());
        if (student.getClassId() == null || !student.getClassId().equals(exam.getClassId())) {
            throw BizException.unprocessable("学生不在这场考试的班级");
        }
        assertExamFields(vo, exam);
        examScoreRepository.findByExamIdAndStudentId(exam.getId(), student.getId()).ifPresent(existing -> {
            throw BizException.conflict("该学生这场考试成绩已存在");
        });
        ExamScore score = new ExamScore();
        score.setExamId(exam.getId());
        score.setStudentId(student.getId());
        score.setScore(normalizeScore(vo.getScore()));
        score.setRemark(limitRemark(vo.getRemark()));
        score = examScoreRepository.save(score);
        return toVo(score, exam);
    }

    @Override
    @Transactional
    public ExamScoreVo update(Long id, ExamScoreVo vo) {
        if (vo == null) {
            throw BizException.badRequest("请求体不能为空");
        }
        ExamScore score = examScoreRepository.findById(id).orElseThrow(() -> BizException.notFound("成绩不存在"));
        accessService.assertStudentVisible(score.getStudentId());
        ExamSchedule exam = examScheduleRepository.findById(score.getExamId())
                .orElseThrow(() -> BizException.notFound("考试安排不存在"));
        if (vo.getExamScheduleId() != null && !vo.getExamScheduleId().equals(score.getExamId())) {
            throw BizException.unprocessable("不能改所属考试");
        }
        if (vo.getStudentId() != null && !vo.getStudentId().equals(score.getStudentId())) {
            throw BizException.unprocessable("不能改学生");
        }
        assertExamFields(vo, exam);
        if (vo.getScore() != null) {
            score.setScore(normalizeScore(vo.getScore()));
        }
        if (vo.getRemark() != null) {
            score.setRemark(limitRemark(vo.getRemark()));
        }
        examScoreRepository.save(score);
        return toVo(score, exam);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ExamScore score = examScoreRepository.findById(id).orElseThrow(() -> BizException.notFound("成绩不存在"));
        accessService.assertStudentVisible(score.getStudentId());
        examScoreRepository.delete(score);
    }

    private void assertExamFields(ExamScoreVo vo, ExamSchedule exam) {
        if (vo.getCourseId() != null && !vo.getCourseId().equals(exam.getCourseId())) {
            throw BizException.unprocessable("课程与考试安排不一致");
        }
        if (vo.getAcademicYear() != null && !vo.getAcademicYear().isBlank()
                && !vo.getAcademicYear().trim().equals(exam.getAcademicYear())) {
            throw BizException.unprocessable("学年与考试安排不一致");
        }
        if (vo.getSemester() != null && !vo.getSemester().isBlank()
                && !vo.getSemester().trim().equals(String.valueOf(exam.getSemester()))) {
            throw BizException.unprocessable("学期与考试安排不一致");
        }
    }

    private boolean matchesExam(ExamSchedule exam, Long courseId, String semester, String academicYear) {
        if (exam == null) {
            return courseId == null && (semester == null || semester.isBlank())
                    && (academicYear == null || academicYear.isBlank());
        }
        if (courseId != null && !courseId.equals(exam.getCourseId())) {
            return false;
        }
        if (academicYear != null && !academicYear.isBlank() && !academicYear.trim().equals(exam.getAcademicYear())) {
            return false;
        }
        return semester == null || semester.isBlank() || semester.trim().equals(String.valueOf(exam.getSemester()));
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

    private BigDecimal normalizeScore(Double raw) {
        if (raw < 0 || raw > 100) {
            throw BizException.unprocessable("分数需在 0 到 100 之间");
        }
        return BigDecimal.valueOf(raw).setScale(2, RoundingMode.HALF_UP);
    }

    private String limitRemark(String remark) {
        if (remark == null) {
            return null;
        }
        String value = remark.trim();
        if (value.length() > 255) {
            throw BizException.badRequest("备注过长");
        }
        return value.isEmpty() ? null : value;
    }

    private double gradePoint(double score) {
        if (score >= 90) {
            return 4.0;
        }
        if (score >= 80) {
            return 3.0;
        }
        if (score >= 70) {
            return 2.0;
        }
        if (score >= 60) {
            return 1.0;
        }
        return 0;
    }

    private ExamScoreVo toVo(ExamScore score, ExamSchedule exam) {
        ExamScoreVo vo = new ExamScoreVo();
        vo.setId(score.getId());
        vo.setStudentId(score.getStudentId());
        vo.setExamScheduleId(score.getExamId());
        vo.setScore(score.getScore() == null ? null : score.getScore().doubleValue());
        vo.setGradePoint(score.getScore() == null ? null : gradePoint(score.getScore().doubleValue()));
        vo.setRemark(score.getRemark());
        vo.setCreatedAt(score.getCreatedAt());
        vo.setUpdatedAt(score.getUpdatedAt());
        if (exam != null) {
            vo.setCourseId(exam.getCourseId());
            vo.setAcademicYear(exam.getAcademicYear());
            vo.setSemester(exam.getSemester() == null ? null : String.valueOf(exam.getSemester()));
            vo.setExamDate(exam.getExamDate());
        }
        return vo;
    }
}
