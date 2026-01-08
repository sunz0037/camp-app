package com.camp.app.service;

import com.camp.app.api.PageResult;
import com.camp.app.model.ClassAttendanceDailyVo;
import com.camp.app.model.StudentAttendanceRecordVo;

public interface AttendanceService {
    PageResult<ClassAttendanceDailyVo> listClassDaily(long page, long size, String grade, String className, String attendanceDate);
    void upsertClassDaily(ClassAttendanceDailyVo vo);

    PageResult<StudentAttendanceRecordVo> listStudentAttendance(long page, long size, String grade, String className, String attendanceDate, Integer status);
    void createStudentAttendance(StudentAttendanceRecordVo vo);
    void updateStudentAttendance(Long id, StudentAttendanceRecordVo vo);
    void deleteStudentAttendance(Long id);
}

