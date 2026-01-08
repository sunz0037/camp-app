# 密码重置功能实现指南

## 一、需求概述

面向初高中学生的密码重置功能，使用**6项信息验证**：
1. 学号/工号
2. 真实姓名
3. 身份证号
4. 应急联系人姓名
5. 应急联系人电话
6. 关系（父亲/母亲/监护人/其他）

**重置规则**：
- 验证通过后，密码自动重置为**身份证号后6位**
- 使用BCrypt加密存储（强度5）
- 不发送短信，仅验证信息匹配

## 二、数据库设计

### 2.1 密码重置日志表

```sql
CREATE TABLE IF NOT EXISTS password_reset_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    user_id BIGINT COMMENT '用户ID（成功时记录）',
    identifier VARCHAR(50) COMMENT '学号/工号',
    identity_type TINYINT COMMENT '身份类型：1-学生，2-教工',
    reset_type TINYINT NOT NULL DEFAULT 1 COMMENT '重置类型：1-六项验证',
    status TINYINT NOT NULL COMMENT '状态：0-失败，1-成功',
    failure_reason VARCHAR(255) COMMENT '失败原因',
    ip_address VARCHAR(50) COMMENT '请求IP',
    user_agent VARCHAR(255) COMMENT '用户代理',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_identifier (identifier),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_identifier_created (identifier, created_at),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='密码重置日志表';
```

## 三、实体类设计

### 3.1 密码重置请求DTO

```java
package com.camp.app.model;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Data
public class ResetPasswordRequest {
    /**
     * 身份类型：1-学生，2-教工
     */
    @NotNull(message = "身份类型不能为空")
    private Integer identityType;
    
    /**
     * 学号/工号
     */
    @NotBlank(message = "学号/工号不能为空")
    private String identifier;
    
    /**
     * 真实姓名
     */
    @NotBlank(message = "真实姓名不能为空")
    private String realName;
    
    /**
     * 身份证号（完整18位）
     */
    @NotBlank(message = "身份证号不能为空")
    @Pattern(regexp = "^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]$", 
             message = "身份证号格式不正确")
    private String idCard;
    
    /**
     * 应急联系人姓名
     */
    @NotBlank(message = "应急联系人姓名不能为空")
    private String contactName;
    
    /**
     * 应急联系人电话
     */
    @NotBlank(message = "应急联系人电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String contactPhone;
    
    /**
     * 关系：父亲、母亲、监护人、其他
     */
    @NotBlank(message = "关系不能为空")
    private String relationship;
}
```

### 3.2 密码重置响应DTO

```java
package com.camp.app.model;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class ResetPasswordResponse {
    private Boolean success;
    private String message;
    private String maskedIdCard; // 脱敏身份证号（仅显示后4位）
}
```

### 3.3 密码重置日志实体

```java
package com.camp.app.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_logs")
@Data
public class PasswordResetLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "identifier", length = 50)
    private String identifier;
    
    @Column(name = "identity_type")
    private Integer identityType;
    
    @Column(name = "reset_type", nullable = false)
    private Integer resetType = 1; // 1-六项验证
    
    @Column(name = "status", nullable = false)
    private Integer status; // 0-失败，1-成功
    
    @Column(name = "failure_reason", length = 255)
    private String failureReason;
    
    @Column(name = "ip_address", length = 50)
    private String ipAddress;
    
    @Column(name = "user_agent", length = 255)
    private String userAgent;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

## 四、Repository层

### 4.1 密码重置日志Repository

```java
package com.camp.app.repository;

import com.camp.app.entity.PasswordResetLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

@Repository
public interface PasswordResetLogRepository extends JpaRepository<PasswordResetLog, Long> {
    /**
     * 统计指定学号/工号在指定时间内的失败次数
     */
    @Query("SELECT COUNT(l) FROM PasswordResetLog l " +
           "WHERE l.identifier = :identifier " +
           "AND l.status = 0 " +
           "AND l.createdAt >= :since")
    long countFailedAttempts(@Param("identifier") String identifier, 
                            @Param("since") LocalDateTime since);
}
```

### 4.2 应急联系人Repository（需要创建）

```java
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
```

## 五、Service层实现

### 5.1 密码重置Service接口

```java
package com.camp.app.service;

import com.camp.app.model.ResetPasswordRequest;
import com.camp.app.model.ResetPasswordResponse;

public interface PasswordResetService {
    /**
     * 重置密码
     * @param request 重置密码请求
     * @param ipAddress 请求IP
     * @param userAgent 用户代理
     * @return 重置结果
     */
    ResetPasswordResponse resetPassword(ResetPasswordRequest request, 
                                        String ipAddress, 
                                        String userAgent);
}
```

### 5.2 密码重置Service实现

```java
package com.camp.app.service.impl;

import com.camp.app.entity.*;
import com.camp.app.model.ResetPasswordRequest;
import com.camp.app.model.ResetPasswordResponse;
import com.camp.app.repository.*;
import com.camp.app.service.PasswordResetService;
import com.camp.app.util.PasswordEncoderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {
    
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final EmergencyContactRepository emergencyContactRepository;
    private final PasswordResetLogRepository passwordResetLogRepository;
    
    // 频率限制：5分钟内最多3次尝试
    private static final int MAX_ATTEMPTS = 3;
    private static final int LOCKOUT_MINUTES = 5;
    
    @Override
    @Transactional
    public ResetPasswordResponse resetPassword(ResetPasswordRequest request, 
                                               String ipAddress, 
                                               String userAgent) {
        log.info("密码重置请求: identityType={}, identifier={}, ip={}", 
                 request.getIdentityType(), request.getIdentifier(), ipAddress);
        
        // 1. 检查频率限制
        checkAttemptLimit(request.getIdentifier());
        
        // 2. 查找用户
        User user = findUser(request);
        if (user == null) {
            logFailedAttempt(null, request, "学号/工号不存在", ipAddress, userAgent);
            throw new RuntimeException("学号/工号不存在");
        }
        
        // 3. 验证真实姓名
        if (!user.getRealName().equals(request.getRealName().trim())) {
            logFailedAttempt(user.getId(), request, "真实姓名不匹配", ipAddress, userAgent);
            throw new RuntimeException("真实姓名不匹配");
        }
        
        // 4. 验证身份证号
        if (!user.getIdCard().equals(request.getIdCard())) {
            logFailedAttempt(user.getId(), request, "身份证号不匹配", ipAddress, userAgent);
            throw new RuntimeException("身份证号不匹配");
        }
        
        // 5. 验证应急联系人信息（仅学生）
        if (request.getIdentityType() == 1) {
            Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("学生信息不存在"));
            
            Optional<EmergencyContact> contactOpt = emergencyContactRepository
                .findByStudentIdAndContactNameAndPhoneAndRelationship(
                    student.getId(),
                    request.getContactName().trim(),
                    request.getContactPhone().trim(),
                    request.getRelationship().trim()
                );
            
            if (contactOpt.isEmpty()) {
                logFailedAttempt(user.getId(), request, "应急联系人信息不匹配", ipAddress, userAgent);
                throw new RuntimeException("应急联系人信息不匹配");
            }
        } else if (request.getIdentityType() == 2) {
            // 教工用户：暂时也使用应急联系人验证
            // 如果教工表没有应急联系人，可以跳过此验证或使用其他方式
            // TODO: 根据实际需求调整教工的验证逻辑
        }
        
        // 6. 所有验证通过，重置密码
        String newPassword = extractLast6Digits(request.getIdCard());
        String encodedPassword = PasswordEncoderUtil.encode(newPassword);
        user.setPassword(encodedPassword);
        userRepository.save(user);
        
        // 7. 记录成功日志
        logSuccessfulReset(user.getId(), request, ipAddress, userAgent);
        
        log.info("密码重置成功: userId={}, identifier={}", user.getId(), request.getIdentifier());
        
        // 8. 返回响应（脱敏身份证号）
        String maskedIdCard = maskIdCard(request.getIdCard());
        
        return ResetPasswordResponse.builder()
                .success(true)
                .message("密码已重置为身份证号后6位，请使用新密码登录")
                .maskedIdCard(maskedIdCard)
                .build();
    }
    
    /**
     * 检查尝试次数限制
     */
    private void checkAttemptLimit(String identifier) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(LOCKOUT_MINUTES);
        long failedCount = passwordResetLogRepository.countFailedAttempts(identifier, since);
        
        if (failedCount >= MAX_ATTEMPTS) {
            throw new RuntimeException(
                String.format("验证失败次数过多，请%d分钟后再试", LOCKOUT_MINUTES)
            );
        }
    }
    
    /**
     * 查找用户
     */
    private User findUser(ResetPasswordRequest request) {
        if (request.getIdentityType() == 1) {
            // 学生：通过学号查找
            Optional<Student> studentOpt = studentRepository.findByStudentNo(request.getIdentifier());
            if (studentOpt.isEmpty()) {
                return null;
            }
            Student student = studentOpt.get();
            return userRepository.findById(student.getUserId()).orElse(null);
        } else if (request.getIdentityType() == 2) {
            // 教工：通过工号查找
            Optional<User> userOpt = userRepository.findByUsernameAndUserType(
                request.getIdentifier(), 2
            );
            return userOpt.orElse(null);
        }
        return null;
    }
    
    /**
     * 提取身份证号后6位
     */
    private String extractLast6Digits(String idCard) {
        if (idCard == null || idCard.length() < 6) {
            throw new RuntimeException("身份证号格式不正确");
        }
        return idCard.substring(idCard.length() - 6);
    }
    
    /**
     * 脱敏身份证号（仅显示后4位）
     */
    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 4) {
            return "****";
        }
        return "****" + idCard.substring(idCard.length() - 4);
    }
    
    /**
     * 记录失败日志
     */
    private void logFailedAttempt(Long userId, ResetPasswordRequest request, 
                                  String reason, String ipAddress, String userAgent) {
        PasswordResetLog log = new PasswordResetLog();
        log.setUserId(userId);
        log.setIdentifier(request.getIdentifier());
        log.setIdentityType(request.getIdentityType());
        log.setStatus(0); // 失败
        log.setFailureReason(reason);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        passwordResetLogRepository.save(log);
    }
    
    /**
     * 记录成功日志
     */
    private void logSuccessfulReset(Long userId, ResetPasswordRequest request, 
                                    String ipAddress, String userAgent) {
        PasswordResetLog log = new PasswordResetLog();
        log.setUserId(userId);
        log.setIdentifier(request.getIdentifier());
        log.setIdentityType(request.getIdentityType());
        log.setStatus(1); // 成功
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        passwordResetLogRepository.save(log);
    }
}
```

## 六、Controller层实现

### 6.1 密码重置Controller

```java
package com.camp.app.controller;

import com.camp.app.model.BaseResponse;
import com.camp.app.model.ResetPasswordRequest;
import com.camp.app.model.ResetPasswordResponse;
import com.camp.app.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PasswordResetController {
    
    private final PasswordResetService passwordResetService;
    
    /**
     * 重置密码
     */
    @PostMapping("/reset-password")
    public BaseResponse<ResetPasswordResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest) {
        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            
            ResetPasswordResponse response = passwordResetService.resetPassword(
                request, ipAddress, userAgent
            );
            
            return BaseResponse.ok(response);
        } catch (RuntimeException e) {
            log.warn("密码重置失败: {}", e.getMessage());
            return BaseResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("密码重置异常", e);
            return BaseResponse.error(500, "系统错误，请稍后重试");
        }
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
```

## 七、前端实现

### 7.1 重置密码页面组件

```vue
<template>
  <div class="reset-password-page">
    <div class="reset-container">
      <h2>忘记密码</h2>
      <p class="subtitle">请填写以下信息以重置密码</p>
      
      <form @submit.prevent="handleReset" class="reset-form">
        <!-- 身份选择 -->
        <div class="form-group">
          <label>身份类型</label>
          <select v-model="formData.identityType" required>
            <option value="1">学生</option>
            <option value="2">教工</option>
          </select>
        </div>
        
        <!-- 学号/工号 -->
        <div class="form-group">
          <label>{{ formData.identityType == 1 ? '学号' : '工号' }}</label>
          <input 
            v-model="formData.identifier" 
            type="text" 
            required
            :placeholder="`请输入${formData.identityType == 1 ? '学号' : '工号'}`"
          />
        </div>
        
        <!-- 真实姓名 -->
        <div class="form-group">
          <label>真实姓名</label>
          <input 
            v-model="formData.realName" 
            type="text" 
            required
            placeholder="请输入真实姓名"
          />
        </div>
        
        <!-- 身份证号 -->
        <div class="form-group">
          <label>身份证号</label>
          <input 
            v-model="formData.idCard" 
            type="text" 
            required
            maxlength="18"
            placeholder="请输入18位身份证号"
          />
        </div>
        
        <!-- 应急联系人姓名 -->
        <div class="form-group">
          <label>应急联系人姓名</label>
          <input 
            v-model="formData.contactName" 
            type="text" 
            required
            placeholder="请输入应急联系人姓名"
          />
        </div>
        
        <!-- 应急联系人电话 -->
        <div class="form-group">
          <label>应急联系人电话</label>
          <input 
            v-model="formData.contactPhone" 
            type="tel" 
            required
            maxlength="11"
            placeholder="请输入11位手机号"
          />
        </div>
        
        <!-- 关系 -->
        <div class="form-group">
          <label>关系</label>
          <select v-model="formData.relationship" required>
            <option value="">请选择</option>
            <option value="父亲">父亲</option>
            <option value="母亲">母亲</option>
            <option value="监护人">监护人</option>
            <option value="其他">其他</option>
          </select>
        </div>
        
        <!-- 错误提示 -->
        <div v-if="errorMessage" class="error-alert">
          {{ errorMessage }}
        </div>
        
        <!-- 提交按钮 -->
        <button type="submit" class="submit-btn" :disabled="loading">
          {{ loading ? '重置中...' : '重置密码' }}
        </button>
      </form>
      
      <!-- 成功提示对话框 -->
      <div v-if="showSuccessDialog" class="success-dialog">
        <div class="dialog-content">
          <h3>密码重置成功</h3>
          <p>密码已重置为身份证号后6位：{{ maskedIdCard }}</p>
          <p class="hint">请使用新密码登录，建议登录后立即修改密码</p>
          <button @click="goToLogin" class="btn-primary">前往登录</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import authAPI from '../api/auth.js'

const router = useRouter()

const formData = ref({
  identityType: 1,
  identifier: '',
  realName: '',
  idCard: '',
  contactName: '',
  contactPhone: '',
  relationship: ''
})

const loading = ref(false)
const errorMessage = ref('')
const showSuccessDialog = ref(false)
const maskedIdCard = ref('')

const handleReset = async () => {
  errorMessage.value = ''
  loading.value = true
  
  try {
    const response = await authAPI.resetPassword(formData.value)
    
    if (response.data.success) {
      maskedIdCard.value = response.data.maskedIdCard
      showSuccessDialog.value = true
    }
  } catch (error) {
    errorMessage.value = error.message || '重置失败，请检查输入信息'
  } finally {
    loading.value = false
  }
}

const goToLogin = () => {
  router.push('/login')
}
</script>
```

### 7.2 API调用

```javascript
// src/api/auth.js
export const resetPassword = async (data) => {
  const response = await fetch(`${API_BASE_URL}/api/auth/reset-password`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(data)
  })
  
  const result = await response.json()
  
  if (result.code !== 200) {
    throw new Error(result.message || '重置失败')
  }
  
  return result
}
```

## 八、测试要点

### 8.1 正常流程测试
1. 输入正确的6项信息，验证密码是否重置
2. 使用新密码（身份证号后6位）登录
3. 验证密码是否正确加密存储

### 8.2 异常流程测试
1. 学号/工号不存在
2. 真实姓名不匹配
3. 身份证号不匹配
4. 应急联系人信息不匹配
5. 验证失败次数过多（5分钟内超过3次）

### 8.3 边界测试
1. 身份证号格式错误
2. 手机号格式错误
3. 空值验证
4. 特殊字符处理

## 九、注意事项

1. **教工用户验证**：如果教工表没有应急联系人字段，需要调整验证逻辑
2. **身份证号格式**：支持18位新身份证号，15位旧身份证号需要转换
3. **密码安全**：重置后强烈建议用户立即修改密码
4. **日志记录**：所有重置操作都要记录日志，便于审计
5. **频率限制**：防止暴力破解，限制尝试次数

## 十、后续优化

1. 添加图形验证码（防止自动化攻击）
2. 添加操作确认（防止误操作）
3. 密码强度提示（建议用户设置强密码）
4. 重置成功后的邮件/短信通知（可选）

