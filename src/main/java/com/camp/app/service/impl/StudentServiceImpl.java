package com.camp.app.service.impl;

import com.camp.app.api.PageResult;
import com.camp.app.entity.Student;
import com.camp.app.entity.Class;
import com.camp.app.model.StudentVo;
import com.camp.app.repository.StudentRepository;
import com.camp.app.repository.ClassRepository;
import com.camp.app.service.StudentService;
import com.camp.app.util.BeanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {
    private final StudentRepository studentRepository;
    private final ClassRepository classRepository;

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
        // 验证维护顺序：必须存在班级
        if (vo.getClassId() == null) {
            throw new RuntimeException("学生必须关联到已存在的班级（classId不能为空）");
        }
        
        Class classEntity = classRepository.findById(vo.getClassId())
            .orElseThrow(() -> new RuntimeException("班级不存在，请先创建班级"));
        
        // 验证班级容量
        Long currentCount = studentRepository.countByClassId(vo.getClassId());
        if (classEntity.getCapacity() != null && currentCount >= classEntity.getCapacity()) {
            throw new RuntimeException("班级容量已满，无法添加更多学生");
        }
        
        Student student = new Student();
        BeanMapper.copyProperties(vo, student);
        
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
        
        // 如果更新了班级，需要验证
        if (vo.getClassId() != null && !vo.getClassId().equals(student.getClassId())) {
            Class newClass = classRepository.findById(vo.getClassId())
                .orElseThrow(() -> new RuntimeException("新班级不存在"));
            
            // 验证新班级容量（排除当前学生）
            Long currentCount = studentRepository.countByClassId(vo.getClassId());
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
            updateClassStudentCount(vo.getClassId());
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

