package com.camp.app.service.impl;

import com.camp.app.api.BizException;
import com.camp.app.api.PageResult;
import com.camp.app.entity.SysTeacherClass;
import com.camp.app.model.TeacherClassVo;
import com.camp.app.repository.ClassRepository;
import com.camp.app.repository.SysTeacherClassRepository;
import com.camp.app.service.TeacherClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherClassServiceImpl implements TeacherClassService {
    private final SysTeacherClassRepository sysTeacherClassRepository;
    private final ClassRepository classRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResult<TeacherClassVo> list(Long teacherId, long page, long size) {
        List<TeacherClassVo> all = sysTeacherClassRepository.findByTeacherId(teacherId).stream().map(this::toVo).toList();
        int from = (int) Math.min((page - 1) * size, all.size());
        int to = (int) Math.min(from + size, all.size());
        return PageResult.<TeacherClassVo>builder().page(page).size(size).total(all.size()).records(all.subList(from, to)).build();
    }

    @Override
    @Transactional
    public void create(Long teacherId, TeacherClassVo vo) {
        Long classId = vo.getClassId();
        if (classId == null) {
            throw BizException.badRequest("classId 不能为空");
        }
        if (classRepository.findById(classId).isEmpty()) {
            throw BizException.notFound("班级不存在");
        }
        boolean exists = sysTeacherClassRepository.findByTeacherId(teacherId).stream()
                .anyMatch(row -> classId.equals(row.getClassId()));
        if (exists) {
            return;
        }
        SysTeacherClass row = new SysTeacherClass();
        row.setTeacherId(teacherId);
        row.setClassId(classId);
        sysTeacherClassRepository.save(row);
    }

    @Override
    @Transactional
    public void delete(Long teacherId, Long id) {
        SysTeacherClass row = sysTeacherClassRepository.findById(id)
                .orElseThrow(() -> BizException.notFound("教师班级授权不存在"));
        if (!teacherId.equals(row.getTeacherId())) {
            throw BizException.forbidden("不能删除其他教师的授权");
        }
        sysTeacherClassRepository.delete(row);
    }

    @Transactional
    public void replace(Long teacherId, List<Long> classIds) {
        if (classIds == null) {
            throw BizException.badRequest("classIds 不能为空");
        }
        for (Long classId : classIds) {
            if (classRepository.findById(classId).isEmpty()) {
                throw BizException.notFound("班级不存在: " + classId);
            }
        }
        sysTeacherClassRepository.deleteByTeacherId(teacherId);
        for (Long classId : classIds.stream().distinct().toList()) {
            SysTeacherClass row = new SysTeacherClass();
            row.setTeacherId(teacherId);
            row.setClassId(classId);
            sysTeacherClassRepository.save(row);
        }
    }

    private TeacherClassVo toVo(SysTeacherClass row) {
        TeacherClassVo vo = new TeacherClassVo();
        vo.setId(row.getId());
        vo.setTeacherId(row.getTeacherId());
        vo.setClassId(row.getClassId());
        classRepository.findById(row.getClassId()).ifPresent(schoolClass -> {
            vo.setClassName(schoolClass.getClassName());
            if (schoolClass.getGrade() != null) {
                vo.setGradeId(schoolClass.getGrade().getId());
                vo.setGrade(schoolClass.getGrade().getGradeName());
            }
        });
        return vo;
    }
}
