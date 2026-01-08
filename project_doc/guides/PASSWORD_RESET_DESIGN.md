# 密码重置功能设计文档

## 一、设计背景

本平台主要面向**初高中学生**，考虑到：
- 学生通常没有手机号或使用邮箱的习惯
- 需要更严格的身份验证确保安全性
- 重置流程应简单易用

## 二、密码重置方案

### 2.1 验证信息（六项验证）

用户需要提供以下**6项信息**进行身份验证：

| 序号 | 验证项 | 数据来源 | 说明 |
|------|--------|----------|------|
| 1 | 学号/工号 | `users.username` 或 `students.student_no` | 唯一标识 |
| 2 | 真实姓名 | `users.real_name` | 必须与学号/工号匹配 |
| 3 | 身份证号 | `users.id_card` | 完整身份证号 |
| 4 | 应急联系人姓名 | `emergency_contacts.contact_name` | 必须与学生关联 |
| 5 | 应急联系人电话 | `emergency_contacts.phone` | 必须与联系人姓名匹配 |
| 6 | 关系 | `emergency_contacts.relationship` | 父亲/母亲/监护人/其他 |

### 2.2 重置流程

```
步骤1：用户点击"忘记密码"
  ↓
步骤2：用户输入6项验证信息
  - 学号/工号
  - 真实姓名
  - 身份证号
  - 应急联系人姓名
  - 应急联系人电话
  - 关系（下拉选择：父亲/母亲/监护人/其他）
  ↓
步骤3：系统验证所有信息是否匹配
  - 验证学号/工号与姓名是否匹配
  - 验证身份证号是否匹配
  - 验证应急联系人信息是否匹配（姓名+电话+关系）
  ↓
步骤4：验证通过后，自动重置密码
  - 密码重置为：身份证号后6位
  - 更新密码（使用BCrypt加密）
  ↓
步骤5：提示用户密码已重置
  - 显示提示："密码已重置为身份证号后6位，请使用新密码登录"
  - 建议用户登录后立即修改密码
```

### 2.3 验证逻辑

#### 2.3.1 学生用户验证

```sql
-- 验证逻辑（伪代码）
1. 根据学号查找学生
   SELECT * FROM students WHERE student_no = ?
   
2. 根据student_id查找用户
   SELECT * FROM users WHERE id = student.user_id
   
3. 验证用户信息
   - users.username = 输入的学号
   - users.real_name = 输入的真实姓名
   - users.id_card = 输入的身份证号
   
4. 验证应急联系人信息
   SELECT * FROM emergency_contacts 
   WHERE student_id = student.id
     AND contact_name = 输入的应急联系人姓名
     AND phone = 输入的应急联系人电话
     AND relationship = 输入的关系
```

#### 2.3.2 教工用户验证

```sql
-- 教工用户可能没有应急联系人，需要调整验证逻辑
-- 方案A：教工使用5项验证（不包含应急联系人）
-- 方案B：教工也填写应急联系人信息（推荐）

-- 如果采用方案B，验证逻辑：
1. 根据工号查找用户
   SELECT * FROM users WHERE username = ? AND user_type = 2
   
2. 验证用户信息
   - users.real_name = 输入的真实姓名
   - users.id_card = 输入的身份证号
   
3. 验证应急联系人信息（如果教工表也有应急联系人字段）
   -- 或者使用其他验证方式
```

### 2.4 密码重置规则

- **新密码**：身份证号后6位
- **加密方式**：BCrypt（强度5）
- **示例**：
  - 身份证号：`110101200001011234`
  - 新密码：`011234`（后6位）
  - 加密后存储：`$2a$05$...`

### 2.5 安全措施

1. **验证失败限制**
   - 同一学号/工号，5分钟内最多尝试3次
   - 超过3次后，锁定30分钟

2. **日志记录**
   - 记录所有重置请求（成功/失败）
   - 记录IP地址、用户代理
   - 记录验证失败的原因

3. **数据验证**
   - 身份证号格式验证（18位）
   - 手机号格式验证（11位）
   - 姓名长度验证（2-50字符）

4. **密码安全提示**
   - 重置成功后，提示用户登录后立即修改密码
   - 提示密码为身份证号后6位，注意保密

## 三、数据库设计

### 3.1 密码重置记录表（password_reset_logs）

```sql
CREATE TABLE IF NOT EXISTS password_reset_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    user_id BIGINT COMMENT '用户ID（成功时记录）',
    identifier VARCHAR(50) COMMENT '学号/工号',
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
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='密码重置日志表';
```

## 四、API接口设计

### 4.1 重置密码请求

```
POST /api/auth/reset-password

请求体：
{
  "identityType": 1,                    // 身份类型：1-学生，2-教工
  "identifier": "20240001",             // 学号/工号
  "realName": "张三",                    // 真实姓名
  "idCard": "110101200001011234",       // 身份证号（完整）
  "contactName": "张父",                 // 应急联系人姓名
  "contactPhone": "13800000001",        // 应急联系人电话
  "relationship": "父亲"                 // 关系：父亲/母亲/监护人/其他
}

响应（成功）：
{
  "code": 200,
  "message": "密码已重置为身份证号后6位，请使用新密码登录",
  "data": {
    "success": true,
    "maskedIdCard": "****1234"          // 脱敏显示：仅显示后4位
  }
}

响应（失败）：
{
  "code": 400,
  "message": "验证失败：学号与姓名不匹配"  // 或其他具体错误原因
}
```

### 4.2 验证失败原因说明

| 错误码 | 错误信息 | 说明 |
|--------|----------|------|
| 400 | 学号/工号不存在 | 输入的学号/工号在系统中不存在 |
| 400 | 学号与姓名不匹配 | 学号/工号与真实姓名不匹配 |
| 400 | 身份证号不匹配 | 身份证号与用户信息不匹配 |
| 400 | 应急联系人信息不匹配 | 联系人姓名/电话/关系不匹配 |
| 400 | 验证失败次数过多，请30分钟后再试 | 5分钟内尝试超过3次 |
| 500 | 系统错误 | 服务器内部错误 |

## 五、实现逻辑

### 5.1 后端实现步骤

#### 步骤1：验证请求频率
```java
// 检查同一学号/工号在5分钟内的尝试次数
// 如果超过3次，返回错误
```

#### 步骤2：查找用户
```java
// 根据身份类型和学号/工号查找用户
if (identityType == 1) {
    // 学生：通过学号查找
    Student student = studentRepository.findByStudentNo(identifier);
    User user = userRepository.findById(student.getUserId());
} else if (identityType == 2) {
    // 教工：通过工号查找
    User user = userRepository.findByUsernameAndUserType(identifier, 2);
}
```

#### 步骤3：验证用户基本信息
```java
// 验证真实姓名
if (!user.getRealName().equals(realName)) {
    logResetAttempt(userId, identifier, "真实姓名不匹配", false);
    throw new RuntimeException("真实姓名不匹配");
}

// 验证身份证号
if (!user.getIdCard().equals(idCard)) {
    logResetAttempt(userId, identifier, "身份证号不匹配", false);
    throw new RuntimeException("身份证号不匹配");
}
```

#### 步骤4：验证应急联系人信息（仅学生）
```java
if (identityType == 1) {
    // 查找应急联系人
    List<EmergencyContact> contacts = emergencyContactRepository
        .findByStudentIdAndContactNameAndPhoneAndRelationship(
            student.getId(), contactName, contactPhone, relationship
        );
    
    if (contacts.isEmpty()) {
        logResetAttempt(userId, identifier, "应急联系人信息不匹配", false);
        throw new RuntimeException("应急联系人信息不匹配");
    }
}
```

#### 步骤5：重置密码
```java
// 提取身份证号后6位
String newPassword = idCard.substring(idCard.length() - 6);

// 使用BCrypt加密
String encodedPassword = PasswordEncoderUtil.encode(newPassword);

// 更新密码
user.setPassword(encodedPassword);
userRepository.save(user);

// 记录成功日志
logResetAttempt(userId, identifier, "密码重置成功", true);
```

### 5.2 前端实现步骤

#### 步骤1：创建重置密码页面
- 表单包含6个输入字段
- 关系字段使用下拉选择
- 添加表单验证

#### 步骤2：提交验证
- 调用重置密码API
- 显示加载状态
- 处理成功/失败响应

#### 步骤3：成功提示
- 显示成功消息
- 提示密码为身份证号后6位
- 引导用户跳转到登录页

## 六、特殊情况处理

### 6.1 教工用户没有应急联系人

**方案A**：教工也填写应急联系人信息（推荐）
- 在教工信息表中添加应急联系人字段
- 或创建教工应急联系人表

**方案B**：教工使用5项验证（不包含应急联系人）
- 仅验证：工号 + 姓名 + 身份证号
- 需要调整验证逻辑

### 6.2 学生有多个应急联系人

- 只要输入的应急联系人信息与任意一个匹配即可
- 不需要指定是哪个联系人

### 6.3 身份证号格式

- 验证身份证号必须是18位
- 支持15位旧身份证号（需要转换）
- 验证身份证号格式（前17位数字，最后1位数字或X）

### 6.4 密码重置后的安全提示

重置成功后，建议：
1. 提示用户密码已重置为身份证号后6位
2. 建议用户登录后立即修改密码
3. 提醒用户注意密码安全

## 七、安全考虑

### 7.1 信息泄露风险

- **风险**：身份证号后6位作为密码，安全性较低
- **缓解措施**：
  - 提示用户登录后立即修改密码
  - 记录重置日志，便于追溯
  - 限制重置频率，防止恶意尝试

### 7.2 验证信息泄露

- **风险**：如果所有6项信息都泄露，可能被恶意重置
- **缓解措施**：
  - 限制重置频率（5分钟内最多3次）
  - 记录详细日志
  - 异常行为告警

### 7.3 数据脱敏

- 返回给前端的身份证号需要脱敏（仅显示后4位）
- 日志中的敏感信息需要脱敏

## 八、测试场景

### 8.1 正常流程测试

1. **学生重置密码**
   - 输入正确的6项信息
   - 验证密码是否重置为身份证号后6位
   - 验证能否使用新密码登录

2. **教工重置密码**
   - 输入正确的信息（根据实际方案）
   - 验证密码重置是否成功

### 8.2 异常流程测试

1. **学号/工号不存在**
2. **真实姓名不匹配**
3. **身份证号不匹配**
4. **应急联系人信息不匹配**
5. **验证失败次数过多**

### 8.3 边界测试

1. **身份证号格式错误**（非18位）
2. **手机号格式错误**（非11位）
3. **姓名包含特殊字符**
4. **关系字段不在允许范围内**

## 九、实现优先级

### 第一阶段（核心功能）
1. ✅ 学生密码重置（6项验证）
2. ✅ 密码重置为身份证号后6位
3. ✅ 基础验证逻辑

### 第二阶段（完善）
4. ✅ 教工密码重置（根据实际需求调整）
5. ✅ 重置日志记录
6. ✅ 频率限制

### 第三阶段（优化）
7. ✅ 前端页面优化
8. ✅ 错误提示优化
9. ✅ 安全提示优化

## 十、总结

### 核心特点
- ✅ 面向初高中学生，无需手机号/邮箱
- ✅ 6项信息验证，安全性较高
- ✅ 流程简单，易于操作
- ✅ 密码自动重置为身份证号后6位

### 注意事项
1. 确保所有学生都填写了应急联系人信息
2. 教工用户的验证方式需要明确（是否有应急联系人）
3. 密码重置后，强烈建议用户立即修改密码
4. 做好日志记录，便于问题排查和安全审计
