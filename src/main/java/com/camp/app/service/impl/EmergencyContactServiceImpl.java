package com.camp.app.service.impl;

import com.camp.app.entity.EmergencyContact;
import com.camp.app.entity.Student;
import com.camp.app.model.EmergencyContactVo;
import com.camp.app.repository.EmergencyContactRepository;
import com.camp.app.repository.StudentRepository;
import com.camp.app.service.EmergencyContactService;
import com.camp.app.util.BeanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 应急联系人服务实现类
 */
@Service
@RequiredArgsConstructor
public class EmergencyContactServiceImpl implements EmergencyContactService {
    
    private final EmergencyContactRepository emergencyContactRepository;
    private final StudentRepository studentRepository;
    
    @Override
    @Transactional(readOnly = true)
    public List<EmergencyContactVo> listByStudentId(Long studentId) {
        List<EmergencyContact> contacts = emergencyContactRepository.findByStudentId(studentId);
        return contacts.stream()
                .map(this::toVo)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public EmergencyContactVo detail(Long id) {
        EmergencyContact contact = emergencyContactRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("应急联系人不存在: " + id));
        return toVo(contact);
    }
    
    @Override
    @Transactional
    public EmergencyContactVo create(EmergencyContactVo vo) {
        // 验证学生是否存在
        if (vo.getStudentId() == null) {
            throw new RuntimeException("学生ID不能为空");
        }
        
        Student student = studentRepository.findById(vo.getStudentId())
                .orElseThrow(() -> new RuntimeException("学生不存在: " + vo.getStudentId()));
        
        // 验证必填字段
        if (vo.getContactName() == null || vo.getContactName().trim().isEmpty()) {
            throw new RuntimeException("联系人姓名不能为空");
        }
        if (vo.getPhone() == null || vo.getPhone().trim().isEmpty()) {
            throw new RuntimeException("联系人电话不能为空");
        }
        if (vo.getRelationship() == null || vo.getRelationship().trim().isEmpty()) {
            throw new RuntimeException("关系不能为空");
        }
        
        EmergencyContact contact = new EmergencyContact();
        BeanMapper.copyProperties(vo, contact);
        
        // 如果设置为主要联系人，需要取消其他主要联系人
        if (vo.getIsPrimary() != null && vo.getIsPrimary() == 1) {
            setPrimaryContact(vo.getStudentId(), null);
        } else {
            contact.setIsPrimary(0);
        }
        
        contact = emergencyContactRepository.save(contact);
        
        // 如果设置为主要联系人，更新
        if (vo.getIsPrimary() != null && vo.getIsPrimary() == 1) {
            setPrimaryContact(vo.getStudentId(), contact.getId());
        }
        
        return toVo(contact);
    }
    
    @Override
    @Transactional
    public void update(Long id, EmergencyContactVo vo) {
        EmergencyContact contact = emergencyContactRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("应急联系人不存在: " + id));
        
        // 验证必填字段
        if (vo.getContactName() != null && vo.getContactName().trim().isEmpty()) {
            throw new RuntimeException("联系人姓名不能为空");
        }
        if (vo.getPhone() != null && vo.getPhone().trim().isEmpty()) {
            throw new RuntimeException("联系人电话不能为空");
        }
        if (vo.getRelationship() != null && vo.getRelationship().trim().isEmpty()) {
            throw new RuntimeException("关系不能为空");
        }
        
        Long oldStudentId = contact.getStudentId();
        Integer oldIsPrimary = contact.getIsPrimary();
        
        BeanMapper.copyProperties(vo, contact);
        contact.setId(id);
        
        // 如果学生ID改变，需要验证新学生是否存在
        if (vo.getStudentId() != null && !vo.getStudentId().equals(oldStudentId)) {
            Student student = studentRepository.findById(vo.getStudentId())
                    .orElseThrow(() -> new RuntimeException("学生不存在: " + vo.getStudentId()));
        }
        
        // 如果设置为主要联系人，需要取消其他主要联系人
        if (vo.getIsPrimary() != null && vo.getIsPrimary() == 1) {
            if (oldIsPrimary == null || oldIsPrimary == 0) {
                // 之前不是主要联系人，现在要设置为主要联系人
                setPrimaryContact(vo.getStudentId() != null ? vo.getStudentId() : oldStudentId, id);
            }
        } else {
            // 取消主要联系人
            contact.setIsPrimary(0);
        }
        
        emergencyContactRepository.save(contact);
    }
    
    @Override
    @Transactional
    public void delete(Long id) {
        EmergencyContact contact = emergencyContactRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("应急联系人不存在: " + id));
        
        emergencyContactRepository.deleteById(id);
    }
    
    @Override
    @Transactional
    public void setPrimary(Long studentId, Long contactId) {
        // 验证学生是否存在
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("学生不存在: " + studentId));
        
        // 验证联系人是否存在且属于该学生
        if (contactId != null) {
            EmergencyContact contact = emergencyContactRepository.findById(contactId)
                    .orElseThrow(() -> new RuntimeException("应急联系人不存在: " + contactId));
            
            if (!contact.getStudentId().equals(studentId)) {
                throw new RuntimeException("该应急联系人不属于该学生");
            }
        }
        
        setPrimaryContact(studentId, contactId);
    }
    
    /**
     * 设置主要联系人（内部方法）
     */
    private void setPrimaryContact(Long studentId, Long contactId) {
        // 取消该学生的所有主要联系人
        List<EmergencyContact> contacts = emergencyContactRepository.findByStudentId(studentId);
        for (EmergencyContact contact : contacts) {
            if (contact.getIsPrimary() != null && contact.getIsPrimary() == 1) {
                contact.setIsPrimary(0);
                emergencyContactRepository.save(contact);
            }
        }
        
        // 设置新的主要联系人
        if (contactId != null) {
            EmergencyContact primaryContact = emergencyContactRepository.findById(contactId)
                    .orElseThrow(() -> new RuntimeException("应急联系人不存在: " + contactId));
            primaryContact.setIsPrimary(1);
            emergencyContactRepository.save(primaryContact);
        }
    }
    
    /**
     * 转换为VO
     */
    private EmergencyContactVo toVo(EmergencyContact contact) {
        EmergencyContactVo vo = new EmergencyContactVo();
        BeanMapper.copyProperties(contact, vo);
        return vo;
    }
}
