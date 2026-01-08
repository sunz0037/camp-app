package com.camp.app.service.impl;

import com.camp.app.api.PageResult;
import com.camp.app.entity.ClassAttendanceDaily;
import com.camp.app.entity.StudentAttendanceRecord;
import com.camp.app.model.ClassAttendanceDailyVo;
import com.camp.app.model.StudentAttendanceRecordVo;
import com.camp.app.repository.ClassAttendanceDailyRepository;
import com.camp.app.repository.StudentAttendanceRecordRepository;
import com.camp.app.service.AttendanceService;
import com.camp.app.util.BeanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {
    private final ClassAttendanceDailyRepository classAttendanceDailyRepository;
    private final StudentAttendanceRecordRepository studentAttendanceRecordRepository;

    @Override
    public PageResult<ClassAttendanceDailyVo> listClassDaily(long page, long size, String grade, String className, String attendanceDate) {
        Pageable pageable = PageRequest.of((int) (page - 1), (int) size);
        LocalDate date = attendanceDate != null ? LocalDate.parse(attendanceDate) : null;
        Page<ClassAttendanceDaily> dailyPage = classAttendanceDailyRepository.findByFilters(grade, className, date, pageable);
        List<ClassAttendanceDailyVo> voList = BeanMapper.mapList(dailyPage.getContent(), ClassAttendanceDailyVo.class);
        
        return PageResult.<ClassAttendanceDailyVo>builder()
                .page(page)
                .size(size)
                .total(dailyPage.getTotalElements())
                .records(voList)
                .build();
    }

    @Override
    @Transactional
    public void upsertClassDaily(ClassAttendanceDailyVo vo) {
        ClassAttendanceDaily daily = classAttendanceDailyRepository
                .findByAttendanceDateAndGradeAndClassName(vo.getAttendanceDate(), vo.getGrade(), vo.getClassName())
                .orElse(new ClassAttendanceDaily());
        
        BeanMapper.copyProperties(vo, daily);
        
        classAttendanceDailyRepository.save(daily);
    }

    @Override
    public PageResult<StudentAttendanceRecordVo> listStudentAttendance(long page, long size, String grade, String className, String attendanceDate, Integer status) {
        Pageable pageable = PageRequest.of((int) (page - 1), (int) size);
        LocalDate date = attendanceDate != null ? LocalDate.parse(attendanceDate) : null;
        Page<StudentAttendanceRecord> recordPage = studentAttendanceRecordRepository.findByFilters(grade, className, date, status, pageable);
        List<StudentAttendanceRecordVo> voList = BeanMapper.mapList(recordPage.getContent(), StudentAttendanceRecordVo.class);
        
        return PageResult.<StudentAttendanceRecordVo>builder()
                .page(page)
                .size(size)
                .total(recordPage.getTotalElements())
                .records(voList)
                .build();
    }

    @Override
    @Transactional
    public void createStudentAttendance(StudentAttendanceRecordVo vo) {
        StudentAttendanceRecord record = BeanMapper.map(vo, StudentAttendanceRecord.class);
        studentAttendanceRecordRepository.save(record);
    }

    @Override
    @Transactional
    public void updateStudentAttendance(Long id, StudentAttendanceRecordVo vo) {
        StudentAttendanceRecord record = studentAttendanceRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("StudentAttendanceRecord not found: " + id));
        BeanMapper.copyProperties(vo, record);
        record.setId(id);
        studentAttendanceRecordRepository.save(record);
    }

    @Override
    @Transactional
    public void deleteStudentAttendance(Long id) {
        studentAttendanceRecordRepository.deleteById(id);
    }
}

