package com.camp.app.service.impl;

import com.camp.app.api.PageResult;
import com.camp.app.entity.ExamScore;
import com.camp.app.model.ExamScoreVo;
import com.camp.app.repository.ExamScoreRepository;
import com.camp.app.service.ExamScoreService;
import com.camp.app.util.BeanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamScoreServiceImpl implements ExamScoreService {
    private final ExamScoreRepository examScoreRepository;

    @Override
    public PageResult<ExamScoreVo> list(long page, long size, Long studentId, Long courseId, Long examScheduleId, String semester, String academicYear, String examType) {
        Pageable pageable = PageRequest.of((int) (page - 1), (int) size);
        Page<ExamScore> scorePage = examScoreRepository.findByFilters(studentId, courseId, examScheduleId, semester, academicYear, examType, pageable);
        List<ExamScoreVo> voList = BeanMapper.mapList(scorePage.getContent(), ExamScoreVo.class);
        
        return PageResult.<ExamScoreVo>builder()
                .page(page)
                .size(size)
                .total(scorePage.getTotalElements())
                .records(voList)
                .build();
    }

    @Override
    public ExamScoreVo detail(Long id) {
        return examScoreRepository.findById(id)
                .map(score -> BeanMapper.map(score, ExamScoreVo.class))
                .orElse(null);
    }

    @Override
    @Transactional
    public void create(ExamScoreVo vo) {
        ExamScore score = BeanMapper.map(vo, ExamScore.class);
        examScoreRepository.save(score);
    }

    @Override
    @Transactional
    public void update(Long id, ExamScoreVo vo) {
        ExamScore score = examScoreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ExamScore not found: " + id));
        BeanMapper.copyProperties(vo, score);
        score.setId(id);
        examScoreRepository.save(score);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        examScoreRepository.deleteById(id);
    }
}

