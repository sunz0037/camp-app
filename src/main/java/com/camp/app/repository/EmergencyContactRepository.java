package com.camp.app.repository;

import com.camp.app.entity.EmergencyContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, Long> {
    /**
     * 根据学生ID查找所有应急联系人
     */
    List<EmergencyContact> findByStudentId(Long studentId);
    
    /**
     * 验证应急联系人信息是否匹配
     */
    Optional<EmergencyContact> findByStudentIdAndContactNameAndPhoneAndRelationship(
        Long studentId, 
        String contactName, 
        String phone, 
        String relationship
    );
}

