package com.camp.app.controller;

import com.camp.app.api.BaseResponse;
import com.camp.app.api.PageResult;
import com.camp.app.api.Paging;
import com.camp.app.model.EmergencyContactVo;
import com.camp.app.model.ImportResultVo;
import com.camp.app.model.StudentVo;
import com.camp.app.security.Phase1Permissions;
import com.camp.app.security.RequirePermission;
import com.camp.app.service.AccessService;
import com.camp.app.service.EmergencyContactService;
import com.camp.app.service.StudentService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

    private final StudentService studentService;
    private final EmergencyContactService emergencyContactService;
    private final AccessService accessService;

    @GetMapping
    public BaseResponse<PageResult<StudentVo>> listStudents(@RequestParam(defaultValue = "1") long page,
                                                          @RequestParam(name = "pageSize", required = false) Long pageSize,
                                                          @RequestParam(defaultValue = "20") long size,
                                                          @RequestParam(required = false) String grade,
                                                          @RequestParam(required = false) String className,
                                                          @RequestParam(required = false) String major,
                                                          @RequestParam(required = false) String keyword) {
        return BaseResponse.ok(studentService.list(Paging.page(page), Paging.size(pageSize, size), grade, className, major, keyword));
    }

    @GetMapping("/import-template")
    @RequirePermission(Phase1Permissions.STUDENT_IMPORT)
    public ResponseEntity<String> importTemplate() {
        return csv(studentService.importTemplate(), "student-import-template.csv");
    }

    @PostMapping("/batch-import")
    @RequirePermission(Phase1Permissions.STUDENT_IMPORT)
    public BaseResponse<ImportResultVo> batchImport(@RequestBody String csv) {
        return BaseResponse.ok(studentService.batchImport(csv));
    }

    @GetMapping("/export")
    @RequirePermission(Phase1Permissions.STUDENT_EXPORT)
    public ResponseEntity<String> export() {
        return csv(studentService.exportCsv(), "students.csv");
    }

    @GetMapping("/{id}")
    public BaseResponse<StudentVo> getStudent(@PathVariable Long id) {
        return BaseResponse.ok(studentService.get(id));
    }

    @PostMapping
    @RequirePermission(Phase1Permissions.STUDENT_WRITE)
    public BaseResponse<StudentVo> createStudent(@RequestBody StudentVo student) {
        return BaseResponse.ok(studentService.create(student));
    }

    @PutMapping("/{id}")
    @RequirePermission(Phase1Permissions.STUDENT_WRITE)
    public BaseResponse<StudentVo> updateStudent(@PathVariable Long id, @RequestBody StudentVo student) {
        return BaseResponse.ok(studentService.update(id, student));
    }

    @DeleteMapping("/{id}")
    @RequirePermission(Phase1Permissions.STUDENT_WRITE)
    public BaseResponse<Void> deleteStudent(@PathVariable Long id) {
        studentService.delete(id);
        return BaseResponse.ok(null);
    }

    @GetMapping("/{studentId}/emergency-contacts")
    public BaseResponse<java.util.List<EmergencyContactVo>> contacts(@PathVariable Long studentId) {
        accessService.assertStudentVisible(studentId);
        return BaseResponse.ok(emergencyContactService.listByStudentId(studentId));
    }

    @PostMapping("/{studentId}/emergency-contacts")
    public BaseResponse<EmergencyContactVo> createContact(@PathVariable Long studentId, @RequestBody EmergencyContactVo vo) {
        accessService.assertStudentVisible(studentId);
        vo.setStudentId(studentId);
        return BaseResponse.ok(emergencyContactService.create(vo));
    }

    private ResponseEntity<String> csv(String body, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(new MediaType("text", "csv"))
                .body(body);
    }
}

