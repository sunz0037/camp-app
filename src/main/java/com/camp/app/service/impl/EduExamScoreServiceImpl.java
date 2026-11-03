package com.camp.app.service.impl;

import com.camp.app.api.BizException;
import com.camp.app.api.PageResult;
import com.camp.app.entity.EduExam;
import com.camp.app.entity.EduExamScore;
import com.camp.app.entity.Student;
import com.camp.app.entity.User;
import com.camp.app.model.DataScope;
import com.camp.app.model.EduExamScoreVo;
import com.camp.app.phase2.Phase2Access;
import com.camp.app.repository.EduExamRepository;
import com.camp.app.repository.EduExamScoreRepository;
import com.camp.app.repository.StudentRepository;
import com.camp.app.repository.UserRepository;
import com.camp.app.service.EduExamScoreService;
import com.camp.app.service.EduExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EduExamScoreServiceImpl implements EduExamScoreService {
    private static final BigDecimal MAX_SCORE = new BigDecimal("150");

    private final EduExamScoreRepository scoreRepository;
    private final EduExamRepository examRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final EduExamService examService;
    private final Phase2Access access;

    @Override
    @Transactional(readOnly = true)
    public PageResult<EduExamScoreVo> list(long page, long size, Long examId, Long studentId, Long classId, String keyword) {
        String role = access.role();
        DataScope scope = access.scope();
        Long effectiveStudentId = studentId;
        if (access.isStudent(role)) {
            if (scope.getStudentId() == null) {
                return PageResult.<EduExamScoreVo>builder().page(page).size(size).total(0).records(List.of()).build();
            }
            if (studentId != null && !studentId.equals(scope.getStudentId())) {
                throw BizException.forbidden("只能查看本人成绩");
            }
            effectiveStudentId = scope.getStudentId();
        }
        Map<Long, EduExam> exams = examRepository.findAll().stream()
                .collect(Collectors.toMap(EduExam::getId, Function.identity()));
        Map<Long, Student> students = studentRepository.findAll().stream()
                .collect(Collectors.toMap(Student::getId, Function.identity()));
        String key = lower(keyword);
        Long filterStudent = effectiveStudentId;
        List<EduExamScore> visible = scoreRepository.findAll().stream()
                .filter(row -> readable(row, exams, students, scope, role))
                .filter(row -> examId == null || examId.equals(row.getExamId()))
                .filter(row -> filterStudent == null || filterStudent.equals(row.getStudentId()))
                .filter(row -> classId == null || classId.equals(classIdOf(students.get(row.getStudentId()))))
                .filter(row -> key == null || hit(row, exams.get(row.getExamId()), students.get(row.getStudentId()), key))
                .sorted(Comparator.comparing(EduExamScore::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        int from = (int) Math.min((page - 1) * size, visible.size());
        int to = (int) Math.min(from + size, visible.size());
        List<EduExamScoreVo> items = visible.subList(from, to).stream()
                .map(row -> toVo(row, exams.get(row.getExamId()), students.get(row.getStudentId())))
                .toList();
        return PageResult.<EduExamScoreVo>builder().page(page).size(size).total(visible.size()).records(items).build();
    }

    @Override
    @Transactional(readOnly = true)
    public EduExamScoreVo get(Long id) {
        EduExamScore score = required(id);
        EduExam exam = examRepository.findById(score.getExamId()).orElseThrow(() -> BizException.notFound("考试不存在"));
        Student student = studentRepository.findById(score.getStudentId()).orElseThrow(() -> BizException.notFound("学生不存在"));
        if (!canRead(exam, student)) {
            throw BizException.forbidden("超出数据范围");
        }
        return toVo(score, exam, student);
    }

    @Override
    @Transactional
    public EduExamScoreVo create(EduExamScoreVo vo) {
        if (vo == null || vo.getExamId() == null || vo.getStudentId() == null) {
            throw BizException.badRequest("考试和学生不能为空");
        }
        EduExam exam = examRepository.findById(vo.getExamId()).orElseThrow(() -> BizException.notFound("考试不存在"));
        Student student = studentRepository.findById(vo.getStudentId()).orElseThrow(() -> BizException.notFound("学生不存在"));
        assertWritable(exam, student);
        if (scoreRepository.findByExamIdAndStudentId(exam.getId(), student.getId()).isPresent()) {
            throw BizException.conflict("该生本场考试已有成绩");
        }
        EduExamScore score = new EduExamScore();
        score.setExamId(exam.getId());
        score.setStudentId(student.getId());
        BigDecimal value = normalize(vo.getScore(), true);
        score.setScore(value);
        score.setGradeLevel(level(vo.getGradeLevel(), value));
        score.setRemark(limit(vo.getRemark(), 255, "备注"));
        score.setRecordedBy(access.userId());
        score.setRecordedAt(LocalDateTime.now());
        return toVo(scoreRepository.save(score), exam, student);
    }

    @Override
    @Transactional
    public EduExamScoreVo update(Long id, EduExamScoreVo vo) {
        if (vo == null) {
            throw BizException.badRequest("请求体不能为空");
        }
        EduExamScore score = required(id);
        Long examId = vo.getExamId() == null ? score.getExamId() : vo.getExamId();
        Long studentId = vo.getStudentId() == null ? score.getStudentId() : vo.getStudentId();
        EduExam exam = examRepository.findById(examId).orElseThrow(() -> BizException.notFound("考试不存在"));
        Student student = studentRepository.findById(studentId).orElseThrow(() -> BizException.notFound("学生不存在"));
        assertWritable(exam, student);
        scoreRepository.findByExamIdAndStudentId(examId, studentId)
                .filter(other -> !other.getId().equals(score.getId()))
                .ifPresent(other -> {
                    throw BizException.conflict("该生本场考试已有成绩");
                });
        score.setExamId(examId);
        score.setStudentId(studentId);
        if (vo.getScore() != null) {
            score.setScore(normalize(vo.getScore(), true));
        }
        if (vo.getGradeLevel() != null) {
            score.setGradeLevel(level(vo.getGradeLevel(), score.getScore()));
        } else if (vo.getScore() != null && (score.getGradeLevel() == null || score.getGradeLevel().isBlank())) {
            score.setGradeLevel(level(null, score.getScore()));
        }
        if (vo.getRemark() != null) {
            score.setRemark(limit(vo.getRemark(), 255, "备注"));
        }
        score.setRecordedBy(access.userId());
        score.setRecordedAt(LocalDateTime.now());
        return toVo(scoreRepository.save(score), exam, student);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        EduExamScore score = required(id);
        EduExam exam = examRepository.findById(score.getExamId()).orElseThrow(() -> BizException.notFound("考试不存在"));
        Student student = studentRepository.findById(score.getStudentId()).orElseThrow(() -> BizException.notFound("学生不存在"));
        assertWritable(exam, student);
        scoreRepository.delete(score);
    }

    private boolean readable(EduExamScore row, Map<Long, EduExam> exams, Map<Long, Student> students, DataScope scope, String role) {
        EduExam exam = exams.get(row.getExamId());
        Student student = students.get(row.getStudentId());
        if (exam == null || student == null) {
            return false;
        }
        if (access.isStudent(role)) {
            return student.getId().equals(scope.getStudentId());
        }
        return canRead(exam, student);
    }

    private boolean canRead(EduExam exam, Student student) {
        String role = access.role();
        DataScope scope = access.scope();
        if (access.isAdmin(role) || scope.isAll()) {
            return true;
        }
        if (access.isStudent(role)) {
            return student.getId().equals(scope.getStudentId());
        }
        if (!examService.visibleToCurrentUser(exam.getId())) {
            return false;
        }
        return studentInScope(student, scope, role);
    }

    private void assertWritable(EduExam exam, Student student) {
        String role = access.role();
        if (access.isStudent(role)) {
            throw BizException.forbidden("学生不能录入成绩");
        }
        if (!canRead(exam, student)) {
            throw BizException.forbidden("超出数据范围");
        }
        if (Boolean.TRUE.equals(student.getIsDeleted())) {
            throw BizException.unprocessable("学生已删除");
        }
    }

    private boolean studentInScope(Student student, DataScope scope, String role) {
        if (student.getClassId() != null && scope.getClassIds() != null && scope.getClassIds().contains(student.getClassId())) {
            return true;
        }
        return access.isAcademic(role) && student.getGradeId() != null && scope.getGradeIds() != null
                && scope.getGradeIds().contains(student.getGradeId());
    }

    private EduExamScore required(Long id) {
        return scoreRepository.findById(id).orElseThrow(() -> BizException.notFound("成绩不存在"));
    }

    private EduExamScoreVo toVo(EduExamScore score, EduExam exam, Student student) {
        EduExamScoreVo vo = new EduExamScoreVo();
        vo.setId(score.getId());
        vo.setExamId(score.getExamId());
        vo.setExamName(exam == null ? null : exam.getExamName());
        vo.setStudentId(score.getStudentId());
        if (student != null) {
            vo.setStudentNo(student.getStudentNo());
            vo.setClassId(student.getClassId());
            vo.setClassName(student.getClassName());
            vo.setGradeId(student.getGradeId());
            if (student.getUserId() != null) {
                userRepository.findById(student.getUserId()).map(User::getRealName).ifPresent(vo::setStudentName);
            }
        }
        vo.setScore(score.getScore());
        vo.setGradeLevel(score.getGradeLevel());
        vo.setRecordedBy(score.getRecordedBy());
        if (score.getRecordedBy() != null) {
            userRepository.findById(score.getRecordedBy()).map(User::getRealName).ifPresent(vo::setRecordedByName);
        }
        vo.setRecordedAt(score.getRecordedAt());
        vo.setRemark(score.getRemark());
        return vo;
    }

    private BigDecimal normalize(BigDecimal score, boolean required) {
        if (score == null) {
            if (required) {
                throw BizException.badRequest("分数不能为空");
            }
            return null;
        }
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(MAX_SCORE) > 0) {
            throw BizException.unprocessable("分数必须在 0 到 150 之间");
        }
        return score.setScale(2, RoundingMode.HALF_UP);
    }

    private String level(String raw, BigDecimal score) {
        if (raw != null && !raw.isBlank()) {
            String value = raw.trim();
            if (value.length() > 10) {
                throw BizException.badRequest("等级过长");
            }
            return value;
        }
        if (score == null) {
            return null;
        }
        if (score.compareTo(new BigDecimal("90")) >= 0) {
            return "A";
        }
        if (score.compareTo(new BigDecimal("80")) >= 0) {
            return "B";
        }
        if (score.compareTo(new BigDecimal("70")) >= 0) {
            return "C";
        }
        if (score.compareTo(new BigDecimal("60")) >= 0) {
            return "D";
        }
        return "E";
    }

    private boolean hit(EduExamScore row, EduExam exam, Student student, String key) {
        if (exam != null && exam.getExamName() != null && exam.getExamName().toLowerCase(Locale.ROOT).contains(key)) {
            return true;
        }
        if (student != null && student.getStudentNo() != null && student.getStudentNo().toLowerCase(Locale.ROOT).contains(key)) {
            return true;
        }
        if (row.getRemark() != null && row.getRemark().toLowerCase(Locale.ROOT).contains(key)) {
            return true;
        }
        if (student != null && student.getUserId() != null) {
            return userRepository.findById(student.getUserId())
                    .map(User::getRealName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).contains(key))
                    .isPresent();
        }
        return false;
    }

    private Long classIdOf(Student student) {
        return student == null ? null : student.getClassId();
    }

    private String limit(String raw, int max, String label) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        if (value.length() > max) {
            throw BizException.badRequest(label + "过长");
        }
        return value;
    }

    private String lower(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }
}
