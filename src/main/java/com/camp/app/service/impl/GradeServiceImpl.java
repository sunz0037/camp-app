package com.camp.app.service.impl;

import com.camp.app.api.BizException;
import com.camp.app.api.PageResult;
import com.camp.app.entity.Grade;
import com.camp.app.model.DataScope;
import com.camp.app.model.GradeVo;
import com.camp.app.model.OptionItem;
import com.camp.app.repository.ClassRepository;
import com.camp.app.repository.GradeRepository;
import com.camp.app.service.AccessService;
import com.camp.app.service.GradeService;
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
 * 年级服务实现
 */
@Service
@RequiredArgsConstructor
public class GradeServiceImpl implements GradeService {
    
    private final GradeRepository gradeRepository;
    private final ClassRepository classRepository;
    private final AccessService accessService;
    
    @Override
    public PageResult<GradeVo> list(long page, long size, String gradeType, Integer status, String keyword) {
        Pageable pageable = PageRequest.of((int) (page - 1), (int) size);
        Page<Grade> gradePage = gradeRepository.findByConditions(gradeType, status, keyword, pageable);
        
        List<GradeVo> voList = gradePage.getContent().stream()
            .map(this::toVo)
            .collect(Collectors.toList());
        
        return PageResult.<GradeVo>builder()
            .records(voList)
            .total(gradePage.getTotalElements())
            .page(page)
            .size(size)
            .build();
    }
    
    @Override
    public GradeVo detail(Long id) {
        Grade grade = gradeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("年级不存在"));
        return toVo(grade);
    }
    
    @Override
    @Transactional
    public void create(GradeVo gradeVo) {
        // 验证年级代码唯一性
        if (gradeRepository.findByGradeCode(gradeVo.getGradeCode()).isPresent()) {
            throw new RuntimeException("年级代码已存在");
        }
        
        // 验证年级类型和级别
        validateGradeTypeAndLevel(gradeVo.getGradeType(), gradeVo.getGradeLevel());
        
        // 验证同类型同级别是否已存在
        if (gradeRepository.findByGradeTypeAndGradeLevel(gradeVo.getGradeType(), gradeVo.getGradeLevel()).isPresent()) {
            throw new RuntimeException("该年级类型和级别已存在");
        }
        
        Grade grade = new Grade();
        BeanMapper.copyProperties(gradeVo, grade);
        gradeRepository.save(grade);
    }
    
    @Override
    @Transactional
    public void update(Long id, GradeVo gradeVo) {
        Grade grade = gradeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("年级不存在"));
        
        // 如果修改了年级代码，验证唯一性
        if (!grade.getGradeCode().equals(gradeVo.getGradeCode())) {
            if (gradeRepository.findByGradeCode(gradeVo.getGradeCode()).isPresent()) {
                throw new RuntimeException("年级代码已存在");
            }
        }
        
        // 验证年级类型和级别
        validateGradeTypeAndLevel(gradeVo.getGradeType(), gradeVo.getGradeLevel());
        
        // 如果修改了类型或级别，验证是否冲突
        if (!grade.getGradeType().equals(gradeVo.getGradeType()) || 
            !grade.getGradeLevel().equals(gradeVo.getGradeLevel())) {
            gradeRepository.findByGradeTypeAndGradeLevel(gradeVo.getGradeType(), gradeVo.getGradeLevel())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new RuntimeException("该年级类型和级别已存在");
                    }
                });
        }
        
        BeanMapper.copyProperties(gradeVo, grade);
        gradeRepository.save(grade);
    }
    
    @Override
    @Transactional
    public void delete(Long id) {
        Grade grade = gradeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("年级不存在"));
        
        if (!classRepository.findByGradeId(id).isEmpty()) {
            throw BizException.conflict("年级下仍有班级，无法删除");
        }
        DataScope scope = accessService.current();
        if (!scope.isAll()) {
            throw BizException.forbidden("仅管理员可删除年级");
        }
        gradeRepository.delete(grade);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OptionItem> options() {
        DataScope scope = accessService.current();
        return gradeRepository.findByStatus(1).stream()
                .filter(grade -> scope.isAll() || (scope.getGradeIds() != null && scope.getGradeIds().contains(grade.getId())))
                .map(grade -> new OptionItem(grade.getId(), grade.getGradeName(), null))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<GradeVo> listAllEnabled() {
        return gradeRepository.findByStatus(1).stream()
            .map(this::toVo)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<GradeVo> listByType(String gradeType) {
        return gradeRepository.findByGradeType(gradeType).stream()
            .map(this::toVo)
            .collect(Collectors.toList());
    }
    
    /**
     * 验证年级类型和级别
     */
    private void validateGradeTypeAndLevel(String gradeType, Integer gradeLevel) {
        if (gradeType == null || gradeLevel == null) {
            throw new RuntimeException("年级类型和级别不能为空");
        }
        
        switch (gradeType) {
            case "PRIMARY":
                if (gradeLevel < 1 || gradeLevel > 6) {
                    throw new RuntimeException("小学年级级别必须在1-6之间");
                }
                break;
            case "JUNIOR":
                if (gradeLevel < 1 || gradeLevel > 3) {
                    throw new RuntimeException("初中年级级别必须在1-3之间");
                }
                break;
            case "SENIOR":
                if (gradeLevel < 1 || gradeLevel > 3) {
                    throw new RuntimeException("高中年级级别必须在1-3之间");
                }
                break;
            default:
                throw new RuntimeException("无效的年级类型，必须是PRIMARY、JUNIOR或SENIOR");
        }
    }
    
    /**
     * 转换为VO
     */
    private GradeVo toVo(Grade grade) {
        GradeVo vo = new GradeVo();
        BeanMapper.copyProperties(grade, vo);
        return vo;
    }
}

