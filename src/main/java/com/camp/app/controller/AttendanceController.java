package com.camp.app.controller;

import com.camp.app.api.BaseResponse;
import com.camp.app.api.PageResult;
import com.camp.app.api.Paging;
import com.camp.app.model.AttendanceSummaryRequest;
import com.camp.app.model.ClassAttendanceDailyVo;
import com.camp.app.model.StudentAttendanceRecordVo;
import com.camp.app.security.Phase1Permissions;
import com.camp.app.security.RequirePermission;
import com.camp.app.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/attendances")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping({"/summaries", "/classes"})
    public BaseResponse<PageResult<ClassAttendanceDailyVo>> listSummaries(@RequestParam(defaultValue = "1") long page,
                                                                          @RequestParam(name = "pageSize", required = false) Long pageSize,
                                                                          @RequestParam(defaultValue = "20") long size,
                                                                          @RequestParam(required = false) Long classId,
                                                                          @RequestParam(required = false) String grade,
                                                                          @RequestParam(required = false) String className,
                                                                          @RequestParam(required = false) String attendanceDate) {
        long pageSizeValue = Paging.size(pageSize, size);
        if (grade != null || className != null) {
            return BaseResponse.ok(attendanceService.listClassDaily(Paging.page(page), pageSizeValue, grade, className, attendanceDate));
        }
        return BaseResponse.ok(attendanceService.listSummaries(Paging.page(page), pageSizeValue, classId, attendanceDate));
    }

    @PostMapping("/summaries")
    @RequirePermission(Phase1Permissions.ATTENDANCE_RECORD)
    public BaseResponse<ClassAttendanceDailyVo> saveSummary(@RequestBody AttendanceSummaryRequest request) {
        return BaseResponse.ok(attendanceService.saveSummary(request));
    }

    @PutMapping("/summaries/{id}")
    @RequirePermission(Phase1Permissions.ATTENDANCE_RECORD)
    public BaseResponse<ClassAttendanceDailyVo> updateSummary(@PathVariable Long id, @RequestBody AttendanceSummaryRequest request) {
        return BaseResponse.ok(attendanceService.updateSummary(id, request));
    }

    @DeleteMapping("/summaries/{id}")
    @RequirePermission(Phase1Permissions.ATTENDANCE_RECORD)
    public BaseResponse<Void> deleteSummary(@PathVariable Long id) {
        attendanceService.deleteSummary(id);
        return BaseResponse.ok(null);
    }

    @PostMapping("/classes")
    @RequirePermission(Phase1Permissions.ATTENDANCE_RECORD)
    public BaseResponse<Void> upsertClassDaily(@RequestBody ClassAttendanceDailyVo daily) {
        attendanceService.upsertClassDaily(daily);
        return BaseResponse.ok(null);
    }

    @GetMapping({"/records", "/students"})
    public BaseResponse<PageResult<StudentAttendanceRecordVo>> listRecords(@RequestParam(defaultValue = "1") long page,
                                                                           @RequestParam(name = "pageSize", required = false) Long pageSize,
                                                                           @RequestParam(defaultValue = "20") long size,
                                                                           @RequestParam(required = false) Long classId,
                                                                           @RequestParam(required = false) Long studentId,
                                                                           @RequestParam(required = false) String grade,
                                                                           @RequestParam(required = false) String className,
                                                                           @RequestParam(required = false) String attendanceDate,
                                                                           @RequestParam(required = false) Integer status) {
        long pageSizeValue = Paging.size(pageSize, size);
        if (grade != null || className != null || status != null) {
            return BaseResponse.ok(attendanceService.listStudentAttendance(Paging.page(page), pageSizeValue, grade, className, attendanceDate, status));
        }
        return BaseResponse.ok(attendanceService.listRecords(Paging.page(page), pageSizeValue, classId, studentId, attendanceDate));
    }

    @PostMapping({"/records", "/students"})
    @RequirePermission(Phase1Permissions.ATTENDANCE_RECORD)
    public BaseResponse<Void> createStudentAttendance(@RequestBody StudentAttendanceRecordVo record) {
        attendanceService.createStudentAttendance(record);
        return BaseResponse.ok(null);
    }

    @PutMapping({"/records/{id}", "/students/{id}"})
    @RequirePermission(Phase1Permissions.ATTENDANCE_RECORD)
    public BaseResponse<Void> updateStudentAttendance(@PathVariable Long id, @RequestBody StudentAttendanceRecordVo record) {
        attendanceService.updateStudentAttendance(id, record);
        return BaseResponse.ok(null);
    }

    @DeleteMapping("/students/{id}")
    @RequirePermission(Phase1Permissions.ATTENDANCE_RECORD)
    public BaseResponse<Void> deleteStudentAttendance(@PathVariable Long id) {
        attendanceService.deleteStudentAttendance(id);
        return BaseResponse.ok(null);
    }
}
