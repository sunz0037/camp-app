# 登录系统使用指南

## 概述

系统支持三类身份登录：
- **1-学生**：使用学号登录
- **2-教工**：使用工号登录
- **3-家长/访客**：仅提供身份选择按钮，不做逻辑实现

## 功能特点

1. **首次登录激活**：学生和教工首次登录需要验证姓名并设置密码
2. **姓名验证**：首次登录时验证学号/工号与姓名是否匹配
3. **密码要求**：首次设置密码时，密码长度不少于6位
4. **BCrypt加密**：所有密码使用BCrypt算法加密存储

## API接口

### 1. 用户登录

**接口**：`POST /api/auth/login`

**请求体**：
```json
{
  "identityType": 1,           // 身份类型：1-学生，2-教工，3-家长/访客
  "identifier": "20240001",    // 唯一标识：学号（学生）或工号（教工）
  "realName": "张三",          // 真实姓名（首次登录时必填，用于验证）
  "password": "123456"         // 密码（首次登录时可为空，正常登录时必填）
}
```

**响应（首次登录）**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "isFirstLogin": true,
    "token": null,
    "refreshToken": null,
    "userInfo": {
      "id": 1,
      "username": "20240001",
      "realName": "张三",
      "userType": 1,
      "avatarUrl": null
    },
    "message": "首次登录，请设置密码（密码长度不少于6位）"
  }
}
```

**响应（正常登录）**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "isFirstLogin": false,
    "token": "mock-jwt-token-1",
    "refreshToken": "mock-refresh-token-1",
    "userInfo": {
      "id": 1,
      "username": "20240001",
      "realName": "张三",
      "userType": 1,
      "avatarUrl": null
    }
  }
}
```

**错误响应（姓名不匹配）**：
```json
{
  "code": 401,
  "message": "学号与姓名不匹配，请确认",
  "data": null
}
```

### 2. 激活用户（首次登录设置密码）

**接口**：`POST /api/auth/activate`

**请求体**：
```json
{
  "identityType": 1,           // 身份类型：1-学生，2-教工
  "identifier": "20240001",    // 唯一标识：学号（学生）或工号（教工）
  "realName": "张三",          // 真实姓名（用于验证）
  "password": "123456"         // 新密码（至少6位）
}
```

**响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "isFirstLogin": false,
    "token": "mock-jwt-token-1",
    "refreshToken": "mock-refresh-token-1",
    "userInfo": {
      "id": 1,
      "username": "20240001",
      "realName": "张三",
      "userType": 1,
      "avatarUrl": null
    },
    "message": "激活成功，已设置密码"
  }
}
```

**错误响应（姓名不匹配）**：
```json
{
  "code": 400,
  "message": "学号与姓名不匹配，请确认",
  "data": null
}
```

**错误响应（密码长度不足）**：
```json
{
  "code": 400,
  "message": "密码长度不能少于6位",
  "data": null
}
```

### 3. 修改密码

**接口**：`PUT /api/auth/password?userId=1&oldPassword=123456&newPassword=NewPass123!`

**响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

## 登录流程

### 学生登录流程

1. **选择身份**：在登录页面选择"学生"身份
2. **输入学号**：输入学号（唯一标识）
3. **首次登录**：
   - 输入姓名进行验证
   - 如果学号与姓名匹配，系统返回 `isFirstLogin: true`
   - 弹出密码设置对话框，要求输入密码（至少6位）
   - 调用激活接口设置密码
   - 激活成功后自动登录
4. **正常登录**：
   - 输入学号和密码
   - 系统验证密码
   - 登录成功，返回Token和用户信息

### 教工登录流程

1. **选择身份**：在登录页面选择"教工"身份
2. **输入工号**：输入工号（唯一标识，存储在users表的username字段）
3. **首次登录**：
   - 输入姓名进行验证
   - 如果工号与姓名匹配，系统返回 `isFirstLogin: true`
   - 弹出密码设置对话框，要求输入密码（至少6位）
   - 调用激活接口设置密码
   - 激活成功后自动登录
4. **正常登录**：
   - 输入工号和密码
   - 系统验证密码
   - 登录成功，返回Token和用户信息

### 家长/访客登录

- 仅提供身份选择按钮
- 点击后提示"功能暂未实现"

## 前端实现建议

### 登录页面布局

```
┌─────────────────────────────┐
│      校园OA系统登录          │
├─────────────────────────────┤
│  [学生] [教工] [家长/访客]   │  ← 身份选择按钮
│                             │
│  学号/工号: [___________]   │
│  姓名:     [___________]    │  ← 首次登录时显示
│  密码:     [___________]    │  ← 正常登录时显示
│                             │
│  [登录]                     │
└─────────────────────────────┘
```

### 前端逻辑

1. **身份选择**：
   - 点击"学生"或"教工"按钮，显示对应的输入框
   - 点击"家长/访客"按钮，显示提示"功能暂未实现"

2. **首次登录检测**：
   - 调用登录接口
   - 如果返回 `isFirstLogin: true`，显示密码设置对话框
   - 用户输入密码后，调用激活接口

3. **密码设置对话框**：
   ```
   ┌─────────────────────────────┐
   │      首次登录，请设置密码     │
   ├─────────────────────────────┤
   │  密码（至少6位）:            │
   │  [___________________]      │
   │                             │
   │  [取消]  [确定]              │
   └─────────────────────────────┘
   ```

## 数据库说明

### 学生信息

- **表名**：`students`
- **学号字段**：`student_no`（唯一）
- **关联用户**：通过 `user_id` 关联到 `users` 表

### 教工信息

- **表名**：`users`
- **用户类型**：`user_type = 2`（教工）
- **工号字段**：存储在 `username` 字段（唯一）

## 错误处理

### 常见错误

1. **学号/工号不存在**
   - 错误信息：`"学号不存在"` 或 `"工号不存在"`
   - 处理：提示用户检查输入

2. **姓名不匹配**
   - 错误信息：`"学号与姓名不匹配，请确认"` 或 `"工号与姓名不匹配，请确认"`
   - 处理：提示用户确认学号/工号和姓名是否正确

3. **密码错误**
   - 错误信息：`"密码错误"`
   - 处理：提示用户重新输入密码

4. **密码长度不足**
   - 错误信息：`"密码长度不能少于6位"`
   - 处理：提示用户输入至少6位密码

5. **用户已禁用**
   - 错误信息：`"用户已被禁用，请联系管理员"`
   - 处理：提示用户联系管理员

## 安全说明

1. **密码加密**：所有密码使用BCrypt算法加密存储（强度5）
2. **姓名验证**：首次登录必须验证姓名，防止误激活
3. **密码要求**：首次设置密码至少6位
4. **传输安全**：生产环境建议使用HTTPS传输

## 测试示例

### 学生首次登录

```bash
# 1. 登录（首次，需要激活）
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "identityType": 1,
    "identifier": "20240001",
    "realName": "张三",
    "password": ""
  }'

# 2. 激活（设置密码）
curl -X POST http://localhost:8080/api/auth/activate \
  -H "Content-Type: application/json" \
  -d '{
    "identityType": 1,
    "identifier": "20240001",
    "realName": "张三",
    "password": "123456"
  }'
```

### 学生正常登录

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "identityType": 1,
    "identifier": "20240001",
    "password": "123456"
  }'
```

### 教工登录

```bash
# 教工登录（工号存储在username字段）
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "identityType": 2,
    "identifier": "T001",
    "realName": "李老师",
    "password": "123456"
  }'
```

## 注意事项

1. **教工工号**：当前实现中，教工的工号存储在 `users` 表的 `username` 字段中
2. **首次登录判断**：系统通过检查密码是否为BCrypt格式来判断是否首次登录
3. **家长/访客**：当前仅提供按钮，不做逻辑实现，后续可根据需求扩展
4. **JWT Token**：当前返回的是Mock Token，后续需要实现真正的JWT生成和验证

