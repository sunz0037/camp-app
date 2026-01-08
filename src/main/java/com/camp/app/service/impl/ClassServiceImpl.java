package com.camp.app.service.impl;

import com.camp.app.api.PageResult;
import com.camp.app.entity.Class;
import com.camp.app.entity.Grade;
import com.camp.app.entity.Student;
import com.camp.app.entity.User;
import com.camp.app.model.ClassVo;
import com.camp.app.repository.ClassRepository;
import com.camp.app.repository.GradeRepository;
import com.camp.app.repository.StudentRepository;
import com.camp.app.repository.UserRepository;
import com.camp.app.service.ClassService;
import com.camp.app.util.BeanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 班级服务实现
 */
@Service
@RequiredArgsConstructor
public class ClassServiceImpl implements ClassService {
    
    private final ClassRepository classRepository;
    private final GradeRepository gradeRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    
    @Override
    public PageResult<ClassVo> list(long page, long size, Long gradeId, String gradeType, Integer status, String keyword) {
        Pageable pageable = PageRequest.of((int) (page - 1), (int) size);
        Page<Class> classPage = classRepository.findByConditions(gradeId, gradeType, status, keyword, pageable);
        
        List<ClassVo> voList = classPage.getContent().stream()
            .map(this::toVo)
            .collect(Collectors.toList());
        
        return PageResult.<ClassVo>builder()
            .records(voList)
            .total(classPage.getTotalElements())
            .page(page)
            .size(size)
            .build();
    }
    
    @Override
    public ClassVo detail(Long id) {
        Class classEntity = classRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("班级不存在"));
        return toVo(classEntity);
    }
    
    @Override
    @Transactional
    public void create(ClassVo classVo) {
        // 验证年级是否存在
        Grade grade = gradeRepository.findById(classVo.getGradeId())
            .orElseThrow(() -> new RuntimeException("年级不存在"));
        
        // 验证班级代码唯一性
        if (classRepository.findByClassCode(classVo.getClassCode()).isPresent()) {
            throw new RuntimeException("班级代码已存在");
        }
        
        // 验证同年级同班级序号是否已存在
        if (classRepository.findByGradeIdAndClassNumber(classVo.getGradeId(), classVo.getClassNumber()).isPresent()) {
            throw new RuntimeException("该年级下该班级序号已存在");
        }
        
        Class classEntity = new Class();
        BeanMapper.copyProperties(classVo, classEntity);
        classEntity.setGrade(grade);
        
        // 设置班主任
        if (classVo.getHeadTeacherId() != null) {
            User headTeacher = userRepository.findById(classVo.getHeadTeacherId())
                .orElseThrow(() -> new RuntimeException("班主任不存在"));
            classEntity.setHeadTeacher(headTeacher);
        }
        
        classRepository.save(classEntity);
    }
    
    @Override
    @Transactional
    public void update(Long id, ClassVo classVo) {
        Class classEntity = classRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("班级不存在"));
        
        Long oldGradeId = classEntity.getGrade().getId();
        
        // 如果修改了年级，验证年级是否存在
        if (!oldGradeId.equals(classVo.getGradeId())) {
            Grade grade = gradeRepository.findById(classVo.getGradeId())
                .orElseThrow(() -> new RuntimeException("年级不存在"));
            classEntity.setGrade(grade);
            
            // 如果修改了年级，需要更新该班级所有学生的年级信息
            updateStudentsGrade(classEntity.getId(), classVo.getGradeId());
        }
        
        // 如果修改了班级代码，验证唯一性
        if (!classEntity.getClassCode().equals(classVo.getClassCode())) {
            if (classRepository.findByClassCode(classVo.getClassCode()).isPresent()) {
                throw new RuntimeException("班级代码已存在");
            }
        }
        
        // 如果修改了班级序号，验证是否冲突
        if (!classEntity.getClassNumber().equals(classVo.getClassNumber())) {
            classRepository.findByGradeIdAndClassNumber(classVo.getGradeId(), classVo.getClassNumber())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new RuntimeException("该年级下该班级序号已存在");
                    }
                });
        }
        
        BeanMapper.copyProperties(classVo, classEntity);
        
        // 更新班主任
        if (classVo.getHeadTeacherId() != null) {
            User headTeacher = userRepository.findById(classVo.getHeadTeacherId())
                .orElseThrow(() -> new RuntimeException("班主任不存在"));
            classEntity.setHeadTeacher(headTeacher);
        } else {
            classEntity.setHeadTeacher(null);
        }
        
        classRepository.save(classEntity);
        
        // 更新班级学生数
        updateClassStudentCount(classEntity.getId());
    }
    
    @Override
    @Transactional
    public void delete(Long id) {
        Class classEntity = classRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("班级不存在"));
        
        // 检查是否有学生关联，如果有则不允许删除
        List<Student> students = studentRepository.findByClassId(id);
        if (!students.isEmpty()) {
            throw new RuntimeException("该班级下还有 " + students.size() + " 名学生，无法删除。请先转移或删除学生");
        }
        
        classRepository.delete(classEntity);
    }
    
    @Override
    public List<ClassVo> listByGradeId(Long gradeId) {
        return classRepository.findByGradeId(gradeId).stream()
            .map(this::toVo)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<ClassVo> listEnabledByGradeId(Long gradeId) {
        return classRepository.findByGradeId(gradeId).stream()
            .filter(c -> c.getStatus() == 1)
            .map(this::toVo)
            .collect(Collectors.toList());
    }
    
    /**
     * 转换为VO
     */
    private ClassVo toVo(Class classEntity) {
        ClassVo vo = new ClassVo();
        BeanMapper.copyProperties(classEntity, vo);
        
        // 设置年级信息
        if (classEntity.getGrade() != null) {
            vo.setGradeId(classEntity.getGrade().getId());
            vo.setGradeCode(classEntity.getGrade().getGradeCode());
            vo.setGradeName(classEntity.getGrade().getGradeName());
            vo.setGradeType(classEntity.getGrade().getGradeType());
        }
        
        // 设置班主任信息
        if (classEntity.getHeadTeacher() != null) {
            vo.setHeadTeacherId(classEntity.getHeadTeacher().getId());
            vo.setHeadTeacherName(classEntity.getHeadTeacher().getRealName());
        }
        
        return vo;
    }
    
    /**
     * 更新班级所有学生的年级信息
     */
    private void updateStudentsGrade(Long classId, Long newGradeId) {
        List<Student> students = studentRepository.findByClassId(classId);
        Grade newGrade = gradeRepository.findById(newGradeId)
            .orElseThrow(() -> new RuntimeException("年级不存在"));
        
        for (Student student : students) {
            student.setGradeEntity(newGrade);
            student.setGradeId(newGradeId);
            student.setGrade(newGrade.getGradeName());
            studentRepository.save(student);
        }
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

