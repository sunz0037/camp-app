package com.camp.app.service.impl;

import com.camp.app.api.PageResult;
import com.camp.app.entity.Student;
import com.camp.app.entity.Class;
import com.camp.app.entity.User;
import com.camp.app.entity.Grade;
import com.camp.app.model.StudentVo;
import com.camp.app.model.UserVo;
import com.camp.app.repository.StudentRepository;
import com.camp.app.repository.ClassRepository;
import com.camp.app.repository.GradeRepository;
import com.camp.app.service.StudentService;
import com.camp.app.service.UserService;
import com.camp.app.util.BeanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
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
        return studentRepository.findById(id)
                .map(this::toVo)
                .orElse(null);
    }

    @Override
    @Transactional
    public void create(StudentVo vo) {
        // 验证学号
        if (!StringUtils.hasText(vo.getStudentNo())) {
            throw new RuntimeException("学号不能为空");
        }
        
        // 验证学号唯一性
        if (studentRepository.findByStudentNo(vo.getStudentNo()).isPresent()) {
            throw new RuntimeException("学号已存在");
        }
        
        // 处理班级关联：优先使用 classId，如果没有则通过 grade 和 className 查找
        Class classEntity = null;
        if (vo.getClassId() != null) {
            // 直接使用提供的 classId
            classEntity = classRepository.findById(vo.getClassId())
                .orElseThrow(() -> new RuntimeException("班级不存在，请先创建班级"));
        } else if (StringUtils.hasText(vo.getGrade()) && StringUtils.hasText(vo.getClassName())) {
            // 通过年级名称和班级名称查找
            // 先查找年级
            Optional<Grade> gradeOpt = gradeRepository.findAll().stream()
                .filter(g -> vo.getGrade().equals(g.getGradeName()) || vo.getGrade().equals(g.getGradeCode()))
                .findFirst();
            
            if (gradeOpt.isEmpty()) {
                throw new RuntimeException("年级不存在，请先创建年级：" + vo.getGrade());
            }
            
            Grade grade = gradeOpt.get();
            
            // 再查找班级
            List<Class> classes = classRepository.findByGradeId(grade.getId());
            Optional<Class> classOpt = classes.stream()
                .filter(c -> vo.getClassName().equals(c.getClassName()) || vo.getClassName().equals(c.getClassCode()))
                .findFirst();
            
            if (classOpt.isEmpty()) {
                throw new RuntimeException("班级不存在，请先创建班级：" + vo.getClassName());
            }
            
            classEntity = classOpt.get();
            vo.setClassId(classEntity.getId()); // 设置 classId 以便后续使用
        } else {
            throw new RuntimeException("学生必须关联到已存在的班级（请提供 classId 或 grade 和 className）");
        }
        
        // 验证班级容量
        Long currentCount = studentRepository.countByClassId(vo.getClassId());
        if (classEntity.getCapacity() != null && currentCount >= classEntity.getCapacity()) {
            throw new RuntimeException("班级容量已满，无法添加更多学生");
        }
        
        // 处理用户创建或关联
        Long userId = vo.getUserId();
        if (userId == null) {
            // 如果没有提供 userId，需要创建用户
            // 注意：这里需要前端提供用户信息（realName等），或者使用学号作为用户名
            // 为了简化，如果前端没有提供用户信息，我们使用学号作为用户名创建用户
            UserVo userVo = new UserVo();
            userVo.setUsername(vo.getStudentNo()); // 使用学号作为用户名
            userVo.setRealName(vo.getRealName() != null ? vo.getRealName() : "学生" + vo.getStudentNo());
            userVo.setUserType(1); // 学生类型
            userVo.setStatus(1); // 默认启用
            
            // 创建用户（UserService 会处理密码等）
            userService.create(userVo);
            
            // 获取刚创建的用户ID
            User createdUser = userRepository.findByUsername(vo.getStudentNo())
                .orElseThrow(() -> new RuntimeException("创建用户失败"));
            userId = createdUser.getId();
        } else {
            // 验证用户是否存在且是学生类型
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            if (user.getUserType() == null || user.getUserType() != 1) {
                throw new RuntimeException("用户类型错误，必须是学生类型");
            }
        }
        
        Student student = new Student();
        BeanMapper.copyProperties(vo, student);
        student.setUserId(userId);
        
        // 设置关联对象
        student.setClassEntity(classEntity);
        if (classEntity.getGrade() != null) {
            student.setGradeEntity(classEntity.getGrade());
        }
        
        studentRepository.save(student);
        
        // 更新班级学生数
        updateClassStudentCount(vo.getClassId());
    }

    @Override
    @Transactional
    public void update(Long id, StudentVo vo) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("学生不存在: " + id));
        
        // 处理班级更新：优先使用 classId，如果没有则通过 grade 和 className 查找
        Class newClass = null;
        Long newClassId = vo.getClassId();
        
        if (newClassId == null && StringUtils.hasText(vo.getGrade()) && StringUtils.hasText(vo.getClassName())) {
            // 通过年级名称和班级名称查找
            Optional<Grade> gradeOpt = gradeRepository.findAll().stream()
                .filter(g -> vo.getGrade().equals(g.getGradeName()) || vo.getGrade().equals(g.getGradeCode()))
                .findFirst();
            
            if (gradeOpt.isPresent()) {
                Grade grade = gradeOpt.get();
                List<Class> classes = classRepository.findByGradeId(grade.getId());
                Optional<Class> classOpt = classes.stream()
                    .filter(c -> vo.getClassName().equals(c.getClassName()) || vo.getClassName().equals(c.getClassCode()))
                    .findFirst();
                
                if (classOpt.isPresent()) {
                    newClass = classOpt.get();
                    newClassId = newClass.getId();
                    vo.setClassId(newClassId);
                }
            }
        } else if (newClassId != null) {
            newClass = classRepository.findById(newClassId)
                .orElseThrow(() -> new RuntimeException("新班级不存在"));
        }
        
        // 如果更新了班级，需要验证
        if (newClassId != null && !newClassId.equals(student.getClassId())) {
            if (newClass == null) {
                newClass = classRepository.findById(newClassId)
                    .orElseThrow(() -> new RuntimeException("新班级不存在"));
            }
            
            // 验证新班级容量（排除当前学生）
            Long currentCount = studentRepository.countByClassId(newClassId);
            if (newClass.getCapacity() != null && currentCount >= newClass.getCapacity()) {
                throw new RuntimeException("目标班级容量已满，无法转移学生");
            }
            
            // 更新关联对象
            student.setClassEntity(newClass);
            if (newClass.getGrade() != null) {
                student.setGradeEntity(newClass.getGrade());
            }
            
            // 更新旧班级和新班级的学生数
            if (student.getClassId() != null) {
                updateClassStudentCount(student.getClassId());
            }
            updateClassStudentCount(newClassId);
        }
        
        BeanMapper.copyProperties(vo, student);
        student.setId(id);
        studentRepository.save(student);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Student student = studentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("学生不存在: " + id));
        
        Long classId = student.getClassId();
        studentRepository.deleteById(id);
        
        // 更新班级学生数
        if (classId != null) {
            updateClassStudentCount(classId);
        }
    }
    
    /**
     * 转换为VO
     */
    private StudentVo toVo(Student student) {
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
            });
        }
        
        return vo;
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

