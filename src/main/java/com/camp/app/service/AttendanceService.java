package com.camp.app.service;

import com.camp.app.api.PageResult;
import com.camp.app.model.AttendanceSummaryRequest;
import com.camp.app.model.ClassAttendanceDailyVo;
import com.camp.app.model.StudentAttendanceRecordVo;

public interface AttendanceService {
    PageResult<ClassAttendanceDailyVo> listClassDaily(long page, long size, String grade, String className, String attendanceDate);
    void upsertClassDaily(ClassAttendanceDailyVo vo);

    PageResult<ClassAttendanceDailyVo> listSummaries(long page, long size, Long classId, String attendanceDate);
    ClassAttendanceDailyVo saveSummary(AttendanceSummaryRequest request);
    ClassAttendanceDailyVo updateSummary(Long id, AttendanceSummaryRequest request);
    void deleteSummary(Long id);

    PageResult<StudentAttendanceRecordVo> listStudentAttendance(long page, long size, String grade, String className, String attendanceDate, Integer status);
    PageResult<StudentAttendanceRecordVo> listRecords(long page, long size, Long classId, Long studentId, String attendanceDate);
    void createStudentAttendance(StudentAttendanceRecordVo vo);
    void updateStudentAttendance(Long id, StudentAttendanceRecordVo vo);
    void deleteStudentAttendance(Long id);
}
