# 首次登录流程修复说明

## 问题描述

在首次登录验证时，如果学生输入了学号和姓名，但还没有设置密码，系统直接提示"密码不能为空"，而不是先提示"该学号对应用户信息已激活，请设置密码"。

## 问题原因

原代码逻辑中，虽然先判断了是否首次登录，但在某些情况下，密码验证逻辑可能会先执行，导致直接提示"密码不能为空"。

## 解决方案

### 1. 优化登录逻辑顺序

**修改文件**: `src/main/java/com/camp/app/service/impl/AuthServiceImpl.java`

**关键改进**：
1. **优先判断首次登录**：在验证密码之前，先判断用户是否为首次登录
2. **明确首次登录标识**：使用 `isFirstLoginUser` 变量明确标识
3. **优化提示信息**：姓名验证成功后，提示"该学号对应用户信息已激活，请设置密码"

### 2. 改进密码判断逻辑

**优化 `isDefaultPassword` 方法**：
- 检查密码是否为 null 或空
- 检查是否为占位符（`DEFAULT_PASSWORD_PLACEHOLDER`）
- 检查是否包含 "placeholder" 关键字
- 检查 BCrypt 格式和长度

### 3. 优化错误提示

**首次登录时的提示信息**：
- 如果未提供姓名：`"首次登录需要提供姓名进行验证，请输入学号对应的姓名"`
- 如果姓名验证成功：`"该学号对应用户信息已激活，请设置密码（密码长度不少于6位）"`

## 修改内容

### 后端修改

```java
// 5. 判断是否首次登录（密码未设置）
// 优先判断是否首次登录，避免直接提示密码不能为空
String encodedPassword = user.getPassword();
boolean isFirstLoginUser = (encodedPassword == null || encodedPassword.isEmpty() || 
    encodedPassword.equals(DEFAULT_PASSWORD_PLACEHOLDER) ||
    isDefaultPassword(encodedPassword));

if (isFirstLoginUser) {
    // 首次登录，需要验证姓名并激活
    
    // 验证姓名（必须提供）
    if (request.getRealName() == null || request.getRealName().trim().isEmpty()) {
        String identifierName = request.getIdentityType() == 1 ? "学号" : "工号";
        throw new RuntimeException(String.format("首次登录需要提供姓名进行验证，请输入%s对应的姓名", identifierName));
    }
    
    // 验证姓名是否匹配
    if (!user.getRealName().equals(request.getRealName().trim())) {
        String errorMsg = request.getIdentityType() == 1 ? 
            "学号与姓名不匹配，请确认" : "工号与姓名不匹配，请确认";
        throw new RuntimeException(errorMsg);
    }
    
    // 姓名验证成功，返回首次登录响应
    String identifierName = request.getIdentityType() == 1 ? "学号" : "工号";
    return LoginResponse.builder()
            .isFirstLogin(true)
            .message(String.format("该%s对应用户信息已激活，请设置密码（密码长度不少于6位）", identifierName))
            .userInfo(...)
            .build();
}

// 6. 正常登录：验证密码（只有已激活用户才需要密码）
if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
    throw new RuntimeException("密码不能为空");
}
```

### 前端修改

**优化错误处理**：
- 当收到"首次登录"相关错误时，自动显示姓名输入框
- 改进错误提示显示

## 登录流程

### 首次登录流程（修复后）

1. **用户输入学号/工号**
2. **系统查找用户**
   - 如果用户不存在 → 提示"学号/工号不存在"
3. **判断是否首次登录**
   - 检查密码是否为占位符或未设置
   - **如果是首次登录**：
     - 检查是否提供了姓名
     - 如果未提供姓名 → 提示"首次登录需要提供姓名进行验证"
     - 如果提供了姓名 → 验证姓名是否匹配
     - 如果姓名不匹配 → 提示"学号/工号与姓名不匹配"
     - **如果姓名匹配** → 返回 `isFirstLogin: true`，提示"该学号对应用户信息已激活，请设置密码"
4. **如果不是首次登录**
   - 检查密码是否为空
   - 如果密码为空 → 提示"密码不能为空"
   - 如果密码不为空 → 验证密码

## 测试场景

### 场景1：首次登录，提供姓名

**请求**：
```json
{
  "identityType": 1,
  "identifier": "20240001",
  "realName": "张三",
  "password": ""
}
```

**预期响应**：
```json
{
  "code": 200,
  "data": {
    "isFirstLogin": true,
    "message": "该学号对应用户信息已激活，请设置密码（密码长度不少于6位）",
    "userInfo": {...}
  }
}
```

### 场景2：首次登录，未提供姓名

**请求**：
```json
{
  "identityType": 1,
  "identifier": "20240001",
  "realName": "",
  "password": ""
}
```

**预期响应**：
```json
{
  "code": 401,
  "message": "首次登录需要提供姓名进行验证，请输入学号对应的姓名"
}
```

### 场景3：首次登录，姓名不匹配

**请求**：
```json
{
  "identityType": 1,
  "identifier": "20240001",
  "realName": "错误姓名",
  "password": ""
}
```

**预期响应**：
```json
{
  "code": 401,
  "message": "学号与姓名不匹配，请确认"
}
```

### 场景4：正常登录，密码为空

**请求**：
```json
{
  "identityType": 1,
  "identifier": "20240006",
  "realName": "",
  "password": ""
}
```

**预期响应**：
```json
{
  "code": 401,
  "message": "密码不能为空"
}
```

## 验证方法

### 1. 测试首次登录

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -H "Origin: http://localhost:3000" \
  -d '{
    "identityType": 1,
    "identifier": "20240001",
    "realName": "张三",
    "password": ""
  }'
```

**预期结果**：
- 返回 `isFirstLogin: true`
- 提示信息："该学号对应用户信息已激活，请设置密码（密码长度不少于6位）"

### 2. 前端测试

1. 访问 http://localhost:3000
2. 选择"学生"身份
3. 输入学号：`20240001`
4. 点击"首次登录？"或直接输入姓名：`张三`
5. 点击"登录"
6. **预期**：弹出密码设置对话框，提示"该学号对应用户信息已激活，请设置密码"

## 关键改进点

1. ✅ **优先判断首次登录**：避免直接提示密码不能为空
2. ✅ **优化提示信息**：明确提示"该学号对应用户信息已激活"
3. ✅ **改进密码判断**：正确识别占位符密码
4. ✅ **优化错误处理**：区分首次登录和正常登录的错误

## 总结

修复后的流程：
1. 先判断是否首次登录（密码未设置）
2. 如果是首次登录，验证姓名
3. 姓名验证成功后，提示"该学号对应用户信息已激活，请设置密码"
4. 只有已激活用户才需要验证密码

这样确保了首次登录用户能够正确看到激活提示，而不是直接看到"密码不能为空"的错误。

