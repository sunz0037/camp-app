package com.camp.app.service.impl;

import com.camp.app.api.BizException;
import com.camp.app.entity.SysAffairsGrade;
import com.camp.app.model.OptionItem;
import com.camp.app.repository.GradeRepository;
import com.camp.app.repository.SysAffairsGradeRepository;
import com.camp.app.service.AcademicGradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AcademicGradeServiceImpl implements AcademicGradeService {
    private final SysAffairsGradeRepository sysAffairsGradeRepository;
    private final GradeRepository gradeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<OptionItem> list(Long userId) {
        return sysAffairsGradeRepository.findByUserId(userId).stream()
                .map(row -> gradeRepository.findById(row.getGradeId())
                        .map(grade -> new OptionItem(grade.getId(), grade.getGradeName(), null))
                        .orElse(new OptionItem(row.getGradeId(), null, null)))
                .toList();
    }

    @Override
    @Transactional
    public void replace(Long userId, List<Long> gradeIds) {
        if (gradeIds == null) {
            throw BizException.badRequest("gradeIds 不能为空");
        }
        for (Long gradeId : gradeIds) {
            if (gradeRepository.findById(gradeId).isEmpty()) {
                throw BizException.notFound("年级不存在: " + gradeId);
            }
        }
        sysAffairsGradeRepository.deleteByUserId(userId);
        for (Long gradeId : gradeIds.stream().distinct().toList()) {
            SysAffairsGrade row = new SysAffairsGrade();
            row.setUserId(userId);
            row.setGradeId(gradeId);
            sysAffairsGradeRepository.save(row);
        }
    }
}
