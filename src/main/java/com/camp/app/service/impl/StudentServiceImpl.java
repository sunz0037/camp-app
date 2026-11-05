package com.camp.app.service.impl;

import com.camp.app.api.BizException;
import com.camp.app.api.PageResult;
import com.camp.app.entity.Student;
import com.camp.app.entity.Class;
import com.camp.app.entity.User;
import com.camp.app.entity.Grade;
import com.camp.app.model.DataScope;
import com.camp.app.model.FileVo;
import com.camp.app.model.ImportResultVo;
import com.camp.app.model.StudentVo;
import com.camp.app.model.UserVo;
import com.camp.app.repository.StudentRepository;
import com.camp.app.repository.ClassRepository;
import com.camp.app.repository.GradeRepository;
import com.camp.app.security.AuthContext;
import com.camp.app.service.AccessService;
import com.camp.app.service.AuditLogService;
import com.camp.app.service.FileStorageService;
import com.camp.app.service.StudentService;
import com.camp.app.service.UserService;
import com.camp.app.util.BeanMapper;
import com.camp.app.util.Masking;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {
    private final StudentRepository studentRepository;
    private final ClassRepository classRepository;
    private final GradeRepository gradeRepository;
    private final com.camp.app.repository.UserRepository userRepository;
    private final UserService userService;
    private final AccessService accessService;
    private final AuditLogService auditLogService;
    private final FileStorageService fileStorageService;

    @Override
    public PageResult<StudentVo> list(long page, long size, String grade, String className, String major, String keyword) {
        Pageable pageable = PageRequest.of((int) (page - 1), (int) size);
        
        Specification<Student> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 支持通过 grade_id 或 grade 字符串查询
            if (grade != null && !grade.isEmpty()) {
                try {
                    Long gradeId = Long.parseLong(grade);
                    predicates.add(cb.equal(root.get("gradeId"), gradeId));
                } catch (NumberFormatException e) {
                    predicates.add(cb.equal(root.get("grade"), grade));
                }
            }
            
            // 支持通过 class_id 或 class_name 字符串查询
            if (className != null && !className.isEmpty()) {
                try {
                    Long classId = Long.parseLong(className);
                    predicates.add(cb.equal(root.get("classId"), classId));
                } catch (NumberFormatException e) {
                    predicates.add(cb.equal(root.get("className"), className));
                }
            }
            
            if (major != null && !major.isEmpty()) {
                predicates.add(cb.equal(root.get("major"), major));
            }
            
            if (keyword != null && !keyword.isEmpty()) {
                String pattern = "%" + keyword + "%";
                predicates.add(cb.or(
                    cb.like(root.get("studentNo"), pattern),
                    cb.like(root.get("grade"), pattern),
                    cb.like(root.get("className"), pattern)
                ));
            }
            predicates.add(cb.or(cb.isNull(root.get("isDeleted")), cb.isFalse(root.get("isDeleted"))));
            DataScope scope = accessService.current();
            if (!scope.isAll()) {
                if ("SELF".equals(scope.getLevel())) {
                    if (scope.getStudentId() == null) {
                        predicates.add(cb.disjunction());
                    } else {
                        predicates.add(cb.equal(root.get("id"), scope.getStudentId()));
                    }
                } else if (scope.getClassIds() == null || scope.getClassIds().isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(root.get("classId").in(scope.getClassIds()));
                }
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        Page<Student> studentPage = studentRepository.findAll(spec, pageable);
        List<StudentVo> voList = studentPage.getContent().stream()
            .map(this::toVo)
            .collect(Collectors.toList());
        
        return PageResult.<StudentVo>builder()
                .page(page)
                .size(size)
                .total(studentPage.getTotalElements())
                .records(voList)
                .build();
    }

    @Override
    public StudentVo get(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> BizException.notFound("学生不存在"));
        if (Boolean.TRUE.equals(student.getIsDeleted())) {
            throw BizException.notFound("学生不存在");
        }
        accessService.assertStudentVisible(id);
        return toVo(student, false);
    }

    @Override
    @Transactional
    public StudentVo create(StudentVo vo) {
        if (vo == null || !StringUtils.hasText(vo.getStudentNo())) {
            throw BizException.badRequest("学号不能为空");
        }
        String studentNo = vo.getStudentNo().trim();
        if (studentNo.length() > 20) {
            throw BizException.unprocessable("学号不能超过 20 位");
        }
        if (studentRepository.findByStudentNo(studentNo).isPresent()) {
            throw BizException.conflict("学号已存在");
        }
        if (vo.getStatus() != null && (vo.getStatus() < 0 || vo.getStatus() > 3)) {
            throw BizException.unprocessable("学籍状态只能是 0 休学、1 在读、2 毕业、3 退学");
        }
        if (vo.getGender() != null && vo.getGender() != 0 && vo.getGender() != 1) {
            throw BizException.unprocessable("性别只能是 0 或 1");
        }
        Class classEntity = resolveClass(vo);
        accessService.assertClassInScope(classEntity);
        Long currentCount = studentRepository.countByClassId(classEntity.getId());
        if (classEntity.getCapacity() != null && currentCount >= classEntity.getCapacity()) {
            throw BizException.conflict("班级容量已满，无法添加更多学生");
        }

        Long userId = vo.getUserId();
        if (userId == null) {
            UserVo userVo = new UserVo();
            userVo.setUsername(studentNo);
            userVo.setRealName(StringUtils.hasText(vo.getRealName()) ? vo.getRealName().trim() : "学生" + studentNo);
            userVo.setUserType(1);
            userVo.setStatus(1);
            userVo.setPhone(vo.getPhone());
            userVo.setIdCard(vo.getIdCard());
            userVo.setRoleCodes(List.of("STUDENT"));
            userId = userService.create(userVo).getId();
        } else {
            User user = userRepository.findById(userId).orElseThrow(() -> BizException.notFound("用户不存在"));
            if (user.getUserType() == null || user.getUserType() != 1) {
                throw BizException.unprocessable("关联用户必须是学生类型");
            }
            if (studentRepository.findByUserId(userId).isPresent()) {
                throw BizException.conflict("该用户已有学籍");
            }
        }

        Student student = new Student();
        student.setUserId(userId);
        student.setStudentNo(studentNo);
        student.setClassEntity(classEntity);
        if (classEntity.getGrade() != null) {
            student.setGradeEntity(classEntity.getGrade());
        }
        student.setMajor(vo.getMajor());
        student.setDepartment(vo.getDepartment());
        student.setEnrollmentDate(vo.getEnrollmentDate());
        student.setGraduationDate(vo.getGraduationDate());
        student.setDormitory(vo.getDormitory());
        student.setAddress(vo.getAddress());
        student.setPoliticalStatus(vo.getPoliticalStatus());
        student.setNationality(StringUtils.hasText(vo.getNationality()) ? vo.getNationality() : "中国");
        student.setGender(vo.getGender());
        student.setBirthDate(vo.getBirthDate());
        student.setStatus(vo.getStatus() == null ? 1 : vo.getStatus());
        student.setIsDeleted(false);
        student = studentRepository.save(student);
        updateClassStudentCount(classEntity.getId());
        auditLogService.record("STUDENT_PROFILE", AuthContext.userId(), "STUDENT", String.valueOf(student.getId()),
                "create studentNo=" + studentNo + " classId=" + classEntity.getId(), true);
        return toVo(student, false);
    }

    @Override
    @Transactional
    public StudentVo update(Long id, StudentVo vo) {
        Student student = studentRepository.findById(id).orElseThrow(() -> BizException.notFound("学生不存在"));
        if (Boolean.TRUE.equals(student.getIsDeleted())) {
            throw BizException.notFound("学生不存在");
        }
        accessService.assertStudentVisible(id);
        if (vo == null) {
            throw BizException.badRequest("请求体不能为空");
        }
        Integer previousStatus = student.getStatus();
        Long previousClassId = student.getClassId();
        if (StringUtils.hasText(vo.getStudentNo()) && !vo.getStudentNo().trim().equals(student.getStudentNo())) {
            if (studentRepository.findByStudentNo(vo.getStudentNo().trim()).isPresent()) {
                throw BizException.conflict("学号已存在");
            }
            student.setStudentNo(vo.getStudentNo().trim());
        }
        if (vo.getStatus() != null) {
            if (vo.getStatus() < 0 || vo.getStatus() > 3) {
                throw BizException.unprocessable("学籍状态只能是 0 休学、1 在读、2 毕业、3 退学");
            }
            student.setStatus(vo.getStatus());
        }
        if (vo.getGender() != null) {
            if (vo.getGender() != 0 && vo.getGender() != 1) {
                throw BizException.unprocessable("性别只能是 0 或 1");
            }
            student.setGender(vo.getGender());
        }
        Class newClass = resolveClassIfPresent(vo);
        if (newClass != null && (previousClassId == null || !newClass.getId().equals(previousClassId))) {
            accessService.assertClassInScope(newClass);
            Long currentCount = studentRepository.countByClassId(newClass.getId());
            if (newClass.getCapacity() != null && currentCount >= newClass.getCapacity()) {
                throw BizException.conflict("目标班级容量已满，无法转移学生");
            }
            student.setClassEntity(newClass);
            if (newClass.getGrade() != null) {
                student.setGradeEntity(newClass.getGrade());
            }
        }
        if (vo.getMajor() != null) {
            student.setMajor(vo.getMajor());
        }
        if (vo.getDepartment() != null) {
            student.setDepartment(vo.getDepartment());
        }
        if (vo.getEnrollmentDate() != null) {
            student.setEnrollmentDate(vo.getEnrollmentDate());
        }
        if (vo.getGraduationDate() != null) {
            student.setGraduationDate(vo.getGraduationDate());
        }
        if (vo.getDormitory() != null) {
            student.setDormitory(vo.getDormitory());
        }
        if (vo.getAddress() != null) {
            student.setAddress(vo.getAddress());
        }
        if (vo.getPoliticalStatus() != null) {
            student.setPoliticalStatus(vo.getPoliticalStatus());
        }
        if (vo.getNationality() != null) {
            student.setNationality(vo.getNationality());
        }
        if (vo.getBirthDate() != null) {
            student.setBirthDate(vo.getBirthDate());
        }
        if (student.getUserId() != null && (StringUtils.hasText(vo.getRealName()) || StringUtils.hasText(vo.getPhone()) || StringUtils.hasText(vo.getIdCard()))) {
            userRepository.findById(student.getUserId()).ifPresent(user -> {
                if (StringUtils.hasText(vo.getRealName())) {
                    user.setRealName(vo.getRealName().trim());
                }
                if (StringUtils.hasText(vo.getPhone())) {
                    user.setPhone(vo.getPhone().trim());
                }
                if (StringUtils.hasText(vo.getIdCard())) {
                    user.setIdCard(vo.getIdCard().trim());
                }
                userRepository.save(user);
            });
        }
        studentRepository.save(student);
        if (previousClassId != null) {
            updateClassStudentCount(previousClassId);
        }
        if (student.getClassId() != null) {
            updateClassStudentCount(student.getClassId());
        }
        if (vo.getStatus() != null && !vo.getStatus().equals(previousStatus)) {
            auditLogService.record("STUDENT_STATUS", AuthContext.userId(), "STUDENT", String.valueOf(id),
                    "status " + previousStatus + " -> " + vo.getStatus(), true);
        } else {
            auditLogService.record("STUDENT_PROFILE", AuthContext.userId(), "STUDENT", String.valueOf(id),
                    "update classId=" + student.getClassId(), true);
        }
        return toVo(student, false);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Student student = studentRepository.findById(id).orElseThrow(() -> BizException.notFound("学生不存在"));
        if (Boolean.TRUE.equals(student.getIsDeleted())) {
            throw BizException.notFound("学生不存在");
        }
        accessService.assertStudentVisible(id);
        Long classId = student.getClassId();
        student.setIsDeleted(true);
        studentRepository.save(student);
        if (classId != null) {
            updateClassStudentCount(classId);
        }
        auditLogService.record("STUDENT_PROFILE", AuthContext.userId(), "STUDENT", String.valueOf(id), "soft-delete", true);
    }

    private Class resolveClass(StudentVo vo) {
        Class classEntity = resolveClassIfPresent(vo);
        if (classEntity == null) {
            throw BizException.badRequest("学生必须关联到已存在的班级（请提供 classId 或 grade 和 className）");
        }
        return classEntity;
    }

    private Class resolveClassIfPresent(StudentVo vo) {
        if (vo.getClassId() != null) {
            return classRepository.findById(vo.getClassId()).orElseThrow(() -> BizException.notFound("班级不存在，请先创建班级"));
        }
        if (!StringUtils.hasText(vo.getGrade()) || !StringUtils.hasText(vo.getClassName())) {
            return null;
        }
        Optional<Grade> gradeOpt = gradeRepository.findAll().stream()
                .filter(g -> vo.getGrade().equals(g.getGradeName()) || vo.getGrade().equals(g.getGradeCode()))
                .findFirst();
        if (gradeOpt.isEmpty()) {
            throw BizException.notFound("年级不存在，请先创建年级：" + vo.getGrade());
        }
        return classRepository.findByGradeId(gradeOpt.get().getId()).stream()
                .filter(c -> vo.getClassName().equals(c.getClassName()) || vo.getClassName().equals(c.getClassCode()))
                .findFirst()
                .orElseThrow(() -> BizException.notFound("班级不存在，请先创建班级：" + vo.getClassName()));
    }
    
    /**
     * 转换为VO
     */
    private StudentVo toVo(Student student) {
        return toVo(student, true);
    }

    private StudentVo toVo(Student student, boolean mask) {
        StudentVo vo = new StudentVo();
        BeanMapper.copyProperties(student, vo);
        
        // 设置关联信息
        if (student.getClassEntity() != null) {
            vo.setClassCode(student.getClassEntity().getClassCode());
            vo.setClassNameFull(student.getClassEntity().getClassName());
        }
        if (student.getGradeEntity() != null) {
            vo.setGradeName(student.getGradeEntity().getGradeName());
        }
        
        // 关联查询用户信息，获取学生姓名
        if (student.getUserId() != null) {
            userRepository.findById(student.getUserId()).ifPresent(user -> {
                vo.setRealName(user.getRealName());
                vo.setUsername(user.getUsername());
                vo.setPhone(mask ? Masking.phone(user.getPhone()) : user.getPhone());
                vo.setIdCard(mask ? Masking.idCard(user.getIdCard()) : user.getIdCard());
            });
        }
        
        return vo;
    }

    @Override
    public String importTemplate() {
        return "studentNo,realName,classId,gender,phone\n";
    }

    @Override
    @Transactional
    public ImportResultVo batchImport(String csv) {
        if (csv == null || csv.isBlank()) {
            throw BizException.badRequest("导入内容为空");
        }
        String[] lines = csv.replace("\r", "").split("\n");
        int success = 0;
        List<ImportResultVo.RowError> errors = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.toLowerCase().startsWith("studentno")) {
                continue;
            }
            String[] cols = line.split(",", -1);
            if (cols.length < 3) {
                errors.add(ImportResultVo.RowError.builder().row(i + 1).message("列不足，至少需要学号、姓名、班级ID").build());
                continue;
            }
            try {
                StudentVo vo = new StudentVo();
                vo.setStudentNo(cols[0].trim());
                vo.setRealName(cols[1].trim());
                vo.setClassId(Long.valueOf(cols[2].trim()));
                if (cols.length > 3 && !cols[3].isBlank()) {
                    vo.setGender(Integer.valueOf(cols[3].trim()));
                }
                if (cols.length > 4 && !cols[4].isBlank()) {
                    vo.setPhone(cols[4].trim());
                }
                create(vo);
                if (vo.getPhone() != null) {
                    studentRepository.findByStudentNo(vo.getStudentNo()).ifPresent(saved ->
                            userRepository.findById(saved.getUserId()).ifPresent(user -> {
                                user.setPhone(vo.getPhone());
                                userRepository.save(user);
                            }));
                }
                success++;
            } catch (Exception ex) {
                errors.add(ImportResultVo.RowError.builder().row(i + 1).message(ex.getMessage()).build());
            }
        }
        auditLogService.record("STUDENT_IMPORT", AuthContext.userId(), "STUDENT", null,
                "success=" + success + ", fail=" + errors.size(), true);
        return ImportResultVo.builder().successCount(success).failCount(errors.size()).errors(errors).build();
    }

    @Override
    @Transactional
    public ImportResultVo importFromFile(Long fileId) {
        if (fileId == null) {
            throw BizException.badRequest("fileId 不能为空");
        }
        return batchImport(fileStorageService.readUtf8(fileId));
    }

    @Override
    @Transactional(readOnly = true)
    public String exportCsv() {
        StringBuilder builder = new StringBuilder("studentNo,realName,classId,className,phone,idCard,status\n");
        list(1, 100, null, null, null, null).getRecords().forEach(vo -> builder
                .append(nullToEmpty(vo.getStudentNo())).append(',')
                .append(nullToEmpty(vo.getRealName())).append(',')
                .append(vo.getClassId() == null ? "" : vo.getClassId()).append(',')
                .append(nullToEmpty(vo.getClassName())).append(',')
                .append(nullToEmpty(vo.getPhone())).append(',')
                .append(nullToEmpty(vo.getIdCard())).append(',')
                .append(vo.getStatus() == null ? "" : vo.getStatus())
                .append('\n'));
        auditLogService.record("STUDENT_EXPORT", AuthContext.userId(), "STUDENT", null, "export", true);
        return builder.toString();
    }

    @Override
    @Transactional
    public FileVo exportStored() {
        String csv = exportCsv();
        return fileStorageService.storeBytes(
                AuthContext.userId(),
                "STUDENT_EXPORT",
                "students.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value.replace(",", " ");
    }
    
    /**
     * 更新班级学生数
     */
    private void updateClassStudentCount(Long classId) {
        Long count = studentRepository.countByClassId(classId);
        classRepository.findById(classId).ifPresent(classEntity -> {
            classEntity.setCurrentCount(count.intValue());
            classRepository.save(classEntity);
        });
    }
}

