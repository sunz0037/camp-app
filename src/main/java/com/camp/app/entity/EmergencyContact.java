package com.camp.app.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "emergency_contacts")
@Data
public class EmergencyContact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "student_id", nullable = false)
    private Long studentId;
    
    @Column(name = "contact_name", nullable = false, length = 50)
    private String contactName;
    
    @Column(name = "relationship", nullable = false, length = 20)
    private String relationship; // 父亲、母亲、监护人、其他
    
    @Column(name = "phone", nullable = false, length = 20)
    private String phone;
    
    @Column(name = "alternate_phone", length = 20)
    private String alternatePhone;
    
    @Column(name = "address", length = 255)
    private String address;
    
    @Column(name = "is_primary", nullable = false)
    private Integer isPrimary; // 0-否，1-是
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

