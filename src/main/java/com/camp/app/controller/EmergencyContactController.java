package com.camp.app.controller;

import com.camp.app.api.BaseResponse;
import com.camp.app.model.EmergencyContactVo;
import com.camp.app.service.EmergencyContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 应急联系人控制器
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/emergency-contacts")
public class EmergencyContactController {
    
    private final EmergencyContactService emergencyContactService;
    
    /**
     * 根据学生ID查询所有应急联系人
     */
    @GetMapping("/by-student/{studentId}")
    public BaseResponse<List<EmergencyContactVo>> listByStudentId(@PathVariable Long studentId) {
        return BaseResponse.ok(emergencyContactService.listByStudentId(studentId));
    }
    
    /**
     * 查询应急联系人详情
     */
    @GetMapping("/{id}")
    public BaseResponse<EmergencyContactVo> detail(@PathVariable Long id) {
        return BaseResponse.ok(emergencyContactService.detail(id));
    }
    
    /**
     * 创建应急联系人
     */
    @PostMapping
    public BaseResponse<EmergencyContactVo> create(@RequestBody EmergencyContactVo vo) {
        return BaseResponse.ok(emergencyContactService.create(vo));
    }
    
    /**
     * 更新应急联系人
     */
    @PutMapping("/{id}")
    public BaseResponse<Void> update(@PathVariable Long id, @RequestBody EmergencyContactVo vo) {
        emergencyContactService.update(id, vo);
        return BaseResponse.ok(null);
    }
    
    /**
     * 删除应急联系人
     */
    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable Long id) {
        emergencyContactService.delete(id);
        return BaseResponse.ok(null);
    }
    
    /**
     * 设置主要联系人
     */
    @PutMapping("/set-primary")
    public BaseResponse<Void> setPrimary(@RequestParam Long studentId, @RequestParam Long contactId) {
        emergencyContactService.setPrimary(studentId, contactId);
        return BaseResponse.ok(null);
    }
}
