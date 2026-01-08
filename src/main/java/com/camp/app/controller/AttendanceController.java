package com.camp.app.controller;

import com.camp.app.api.BaseResponse;
import com.camp.app.api.PageResult;
import com.camp.app.model.ClassAttendanceDailyVo;
import com.camp.app.model.StudentAttendanceRecordVo;
import com.camp.app.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    // 班级每日汇总
    @GetMapping("/classes")
    public BaseResponse<PageResult<ClassAttendanceDailyVo>> listClassDaily(@RequestParam(defaultValue = "1") long page,
                                                                         @RequestParam(defaultValue = "20") long size,
                                                                         @RequestParam(required = false) String grade,
                                                                         @RequestParam(required = false) String className,
                                                                         @RequestParam(required = false) String attendanceDate) {
        return BaseResponse.ok(attendanceService.listClassDaily(page, size, grade, className, attendanceDate));
    }

    @PostMapping("/classes")
    public BaseResponse<Void> upsertClassDaily(@RequestBody ClassAttendanceDailyVo daily) {
        attendanceService.upsertClassDaily(daily);
        return BaseResponse.ok(null);
    }

    // 学生异常考勤（仅记录缺勤/迟到/请假/早退）
    @GetMapping("/students")
    public BaseResponse<PageResult<StudentAttendanceRecordVo>> listStudentAttendance(@RequestParam(defaultValue = "1") long page,
                                                                                   @RequestParam(defaultValue = "20") long size,
                                                                                   @RequestParam(required = false) String grade,
                                                                                   @RequestParam(required = false) String className,
                                                                                   @RequestParam(required = false) String attendanceDate,
                                                                                   @RequestParam(required = false) Integer status) {
        return BaseResponse.ok(attendanceService.listStudentAttendance(page, size, grade, className, attendanceDate, status));
    }

    @PostMapping("/students")
    public BaseResponse<Void> createStudentAttendance(@RequestBody StudentAttendanceRecordVo record) {
        attendanceService.createStudentAttendance(record);
        return BaseResponse.ok(null);
    }

    @PutMapping("/students/{id}")
    public BaseResponse<Void> updateStudentAttendance(@PathVariable Long id, @RequestBody StudentAttendanceRecordVo record) {
        attendanceService.updateStudentAttendance(id, record);
        return BaseResponse.ok(null);
    }

    @DeleteMapping("/students/{id}")
    public BaseResponse<Void> deleteStudentAttendance(@PathVariable Long id) {
        attendanceService.deleteStudentAttendance(id);
        return BaseResponse.ok(null);
    }
}

