# BCrypt密码加密使用说明

## 概述

项目已集成BCrypt密码加密算法，用于用户登录校验和密码管理。

## 技术实现

### 1. 依赖配置

项目已包含Spring Security Crypto依赖（`build.gradle`）：
```gradle
implementation 'org.springframework.security:spring-security-crypto'
```

### 2. 核心工具类

**`PasswordEncoderUtil`** - 密码加密工具类
- 位置：`com.camp.app.util.PasswordEncoderUtil`
- 功能：
  - `encode(String rawPassword)` - 加密密码
  - `matches(String rawPassword, String encodedPassword)` - 验证密码
  - `checkPasswordStrength(String password)` - 检查密码强度
  - `needsUpgrade(String encodedPassword)` - 检查是否需要升级加密强度

### 3. 认证服务

**`AuthService`** - 认证服务接口
- 位置：`com.camp.app.service.AuthService`
- 实现：`com.camp.app.service.impl.AuthServiceImpl`
- 功能：
  - 用户登录（使用BCrypt验证密码）
  - 修改密码（使用BCrypt加密新密码）

## 使用示例

### 1. 用户登录

**接口**：`POST /api/auth/login`

**请求**：
```json
{
  "username": "testuser",
  "password": "123456"
}
```

**响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "mock-jwt-token-1",
    "refreshToken": "mock-refresh-token-1",
    "userInfo": {
      "id": 1,
      "username": "testuser",
      "realName": "测试用户",
      "userType": 1,
      "avatarUrl": null
    }
  }
}
```

**流程**：
1. 根据用户名查询用户
2. 检查用户状态（是否启用）
3. 使用 `PasswordEncoderUtil.matches()` 验证密码
4. 返回Token和用户信息

### 2. 修改密码

**接口**：`PUT /api/auth/password?userId=1&oldPassword=123456&newPassword=NewPass123!`

**流程**：
1. 验证旧密码（使用BCrypt）
2. 检查新密码强度
3. 使用 `PasswordEncoderUtil.encode()` 加密新密码
4. 保存到数据库

### 3. 创建用户时加密密码

```java
import com.camp.app.util.PasswordEncoderUtil;

// 创建用户时
String rawPassword = "123456";
String encodedPassword = PasswordEncoderUtil.encode(rawPassword);
user.setPassword(encodedPassword);
userRepository.save(user);
```

## BCrypt特点

1. **自动加盐**：每次加密都会生成随机盐值，相同密码加密结果不同
2. **单向哈希**：无法逆向解密，只能通过验证匹配
3. **自适应强度**：可调整计算成本（当前设置为5，适合开发测试）
4. **安全可靠**：广泛使用，经过充分验证

## 密码强度要求

修改密码时会检查密码强度：
- **弱密码（0）**：长度<8位
- **中等强度（1）**：满足3-4个条件（长度≥12、大小写字母、数字、特殊字符）
- **强密码（2）**：满足5个条件

**建议**：至少8位，包含大小写字母、数字和特殊字符

## 安全最佳实践

1. ✅ **永远不存储明文密码** - 所有密码都使用BCrypt加密
2. ✅ **使用matches方法验证** - 不要直接比较字符串
3. ✅ **检查密码强度** - 修改密码时验证强度
4. ✅ **检查用户状态** - 登录时验证用户是否启用
5. ✅ **记录登录日志** - 记录成功和失败的登录尝试
6. ⚠️ **传输加密** - 生产环境使用HTTPS传输密码
7. ⚠️ **JWT Token** - 后续实现JWT Token机制（当前为Mock）

## 测试工具

可以使用 `PasswordEncoderTest` 工具类测试BCrypt功能：

```bash
cd /Users/sunz/AIProject/camp-app
./gradlew run --args="com.camp.app.util.PasswordEncoderTest"
```

## 数据库密码格式

BCrypt加密后的密码格式：
```
$2a$05$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
```

格式说明：
- `$2a$` - BCrypt版本标识
- `05` - 强度（2^5 = 32轮，当前配置）
- 后面是盐值和哈希值（共60个字符）

## 注意事项

1. **密码长度**：数据库字段 `password` 设置为 `VARCHAR(255)`，足够存储BCrypt加密结果（60字符）
2. **性能**：
   - **强度5**（当前配置）：每次加密约10-20ms，适合开发测试环境
   - **强度10**：每次加密约100-200ms，推荐生产环境使用
   - 验证速度与加密速度相近
3. **迁移**：如果已有明文密码，需要批量加密后更新数据库
4. **安全建议**：生产环境建议将强度调整为10或更高，以提高安全性

## 后续改进

- [ ] 实现JWT Token生成和验证
- [ ] 实现Token刷新机制
- [ ] 实现Token黑名单（登出功能）
- [ ] 添加登录失败次数限制
- [ ] 添加验证码机制
- [ ] 实现密码找回功能

