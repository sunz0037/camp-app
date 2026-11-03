package com.camp.app.service.impl;

import com.camp.app.api.BizException;
import com.camp.app.api.PageResult;
import com.camp.app.entity.ExamSchedule;
import com.camp.app.entity.User;
import com.camp.app.model.ExamScheduleVo;
import com.camp.app.repository.ExamScheduleRepository;
import com.camp.app.repository.UserRepository;
import com.camp.app.service.ExamScheduleService;
import com.camp.app.util.BeanMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 考试安排服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamScheduleServiceImpl implements ExamScheduleService {
    
    private final ExamScheduleRepository examScheduleRepository;
    private final UserRepository userRepository;
    
    @Override
    @Transactional(readOnly = true)
    public PageResult<ExamScheduleVo> list(Integer page, Integer size, String targetGrade, 
                                           String examType, Boolean isPublic, Integer status, 
                                           String keyword) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "examDate"));
        Page<ExamSchedule> examSchedulePage;
        
        // 构建查询条件
        if (StringUtils.hasText(keyword)) {
            // 关键词搜索（考试名称）
            examSchedulePage = examScheduleRepository.findAll(pageable);
            // 这里可以添加更复杂的搜索逻辑
        } else if (StringUtils.hasText(targetGrade) && StringUtils.hasText(examType)) {
            examSchedulePage = examScheduleRepository.findByTargetGradeAndExamType(targetGrade, examType, pageable);
        } else if (StringUtils.hasText(targetGrade)) {
            examSchedulePage = examScheduleRepository.findByTargetGrade(targetGrade, pageable);
        } else if (StringUtils.hasText(examType)) {
            examSchedulePage = examScheduleRepository.findByExamType(examType, pageable);
        } else if (isPublic != null) {
            examSchedulePage = examScheduleRepository.findByIsPublic(isPublic, pageable);
        } else if (status != null) {
            examSchedulePage = examScheduleRepository.findByStatus(status, pageable);
        } else {
            examSchedulePage = examScheduleRepository.findAll(pageable);
        }
        
        // 过滤关键词（如果提供）
        if (StringUtils.hasText(keyword)) {
            List<ExamSchedule> filtered = examSchedulePage.getContent().stream()
                .filter(e -> e.getExamName().contains(keyword))
                .collect(Collectors.toList());
            // 这里简化处理，实际应该用更复杂的查询
        }
        
        List<ExamScheduleVo> voList = examSchedulePage.getContent().stream()
            .map(this::convertToVo)
            .collect(Collectors.toList());
        
        return PageResult.<ExamScheduleVo>builder()
            .page(page)
            .size(size)
            .total(examSchedulePage.getTotalElements())
            .records(voList)
            .build();
    }
    
    @Override
    @Transactional(readOnly = true)
    public ExamScheduleVo getById(Long id) {
        ExamSchedule examSchedule = examScheduleRepository.findById(id)
            .orElseThrow(() -> BizException.notFound("考试安排不存在"));
        return convertToVo(examSchedule);
    }
    
    @Override
    @Transactional
    public ExamScheduleVo create(ExamScheduleVo vo, Long createdBy) {
        // 验证创建人是否为教工
        User creator = userRepository.findById(createdBy)
            .orElseThrow(() -> BizException.notFound("创建人不存在"));
        if (creator.getUserType() == null || (creator.getUserType() != 2 && creator.getUserType() != 3)) {
            throw BizException.forbidden("只有教务或管理员可以创建考试安排");
        }
        if (vo == null || vo.getExamName() == null || vo.getExamName().isBlank() || vo.getExamDate() == null) {
            throw BizException.badRequest("考试名称和时间不能为空");
        }
        String examType = normalizeExamType(vo.getExamType());
        if (examType == null) {
            throw BizException.unprocessable("考试类型必须是月考、期中、期末（或 MONTHLY、MIDTERM、FINAL）");
        }
        if (!isValidGrade(vo.getTargetGrade())) {
            throw BizException.badRequest("年级不能为空");
        }
        vo.setExamType(examType);
        if (vo.getIsPublic() == null) {
            vo.setIsPublic(false);
        }
        
        ExamSchedule examSchedule = BeanMapper.map(vo, ExamSchedule.class);
        examSchedule.setCreatedBy(createdBy);
        examSchedule.setStatus(1); // 默认正常状态
        
        examSchedule = examScheduleRepository.save(examSchedule);
        log.info("创建考试安排成功: id={}, examName={}, createdBy={}", 
            examSchedule.getId(), examSchedule.getExamName(), createdBy);
        
        return convertToVo(examSchedule);
    }
    
    @Override
    @Transactional
    public ExamScheduleVo update(Long id, ExamScheduleVo vo) {
        ExamSchedule examSchedule = examScheduleRepository.findById(id)
            .orElseThrow(() -> BizException.notFound("考试安排不存在"));
        
        // 验证考试类型
        if (vo.getExamType() != null && normalizeExamType(vo.getExamType()) == null) {
            throw BizException.unprocessable("考试类型必须是月考、期中、期末（或 MONTHLY、MIDTERM、FINAL）");
        }
        if (vo.getExamType() != null) {
            vo.setExamType(normalizeExamType(vo.getExamType()));
        }
        if (vo.getTargetGrade() != null && !isValidGrade(vo.getTargetGrade())) {
            throw BizException.badRequest("年级不能为空");
        }
        
        BeanMapper.copyProperties(vo, examSchedule);
        examSchedule = examScheduleRepository.save(examSchedule);
        log.info("更新考试安排成功: id={}", id);
        
        return convertToVo(examSchedule);
    }
    
    @Override
    @Transactional
    public void delete(Long id) {
        ExamSchedule examSchedule = examScheduleRepository.findById(id)
            .orElseThrow(() -> BizException.notFound("考试安排不存在"));
        
        // 检查是否有成绩关联
        // TODO: 可以添加检查逻辑，如果有成绩关联，不允许删除或提示
        
        examScheduleRepository.delete(examSchedule);
        log.info("删除考试安排成功: id={}", id);
    }
    
    @Override
    @Transactional
    public ExamScheduleVo cancel(Long id) {
        ExamSchedule examSchedule = examScheduleRepository.findById(id)
            .orElseThrow(() -> BizException.notFound("考试安排不存在"));
        
        examSchedule.setStatus(0);
        examSchedule = examScheduleRepository.save(examSchedule);
        log.info("取消考试安排成功: id={}", id);
        
        return convertToVo(examSchedule);
    }
    
    /**
     * 转换为VO
     */
    private ExamScheduleVo convertToVo(ExamSchedule examSchedule) {
        ExamScheduleVo vo = BeanMapper.map(examSchedule, ExamScheduleVo.class);
        
        // 填充创建人姓名
        if (examSchedule.getCreatedBy() != null) {
            userRepository.findById(examSchedule.getCreatedBy())
                .ifPresent(user -> vo.setCreatedByName(user.getRealName()));
        }
        
        return vo;
    }
    
    /**
     * 验证考试类型
     */
    private String normalizeExamType(String examType) {
        if (examType == null || examType.isBlank()) {
            return null;
        }
        return switch (examType.trim().toUpperCase()) {
            case "月考", "MONTHLY" -> "月考";
            case "期中", "MIDTERM" -> "期中";
            case "期末", "FINAL" -> "期末";
            default -> null;
        };
    }

    private boolean isValidExamType(String examType) {
        return normalizeExamType(examType) != null;
    }
    
    /**
     * 验证年级格式
     * 现在年级从年级管理模块获取，只需要验证不为空即可
     * 如果需要更严格的验证，可以从年级管理服务中查询年级是否存在
     */
    private boolean isValidGrade(String grade) {
        if (grade == null || grade.isEmpty()) {
            return false;
        }
        // 年级名称从年级管理模块获取，这里只做非空验证
        // 如果需要，可以调用GradeService验证年级是否存在
        return true;
    }
}

