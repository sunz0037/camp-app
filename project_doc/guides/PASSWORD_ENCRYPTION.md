# 密码加密方案推荐

## 推荐算法：BCrypt

### 为什么选择BCrypt？

1. **安全性高**
   - 基于Blowfish加密算法
   - 自适应哈希算法，可调整计算成本（work factor）
   - 自动生成随机盐值（salt），防止彩虹表攻击
   - 每次加密结果都不同，即使密码相同

2. **Spring Boot原生支持**
   - Spring Security内置BCryptPasswordEncoder
   - 无需额外依赖
   - 开箱即用

3. **行业标准**
   - 广泛使用，经过充分验证
   - OWASP推荐
   - 适合生产环境

4. **性能可控**
   - 通过strength参数控制计算强度（4-31，默认10）
   - 平衡安全性和性能

### 其他可选算法对比

| 算法 | 优点 | 缺点 | 推荐度 |
|------|------|------|--------|
| **BCrypt** | Spring内置、自动加盐、自适应 | - | ⭐⭐⭐⭐⭐ 强烈推荐 |
| Argon2 | 最新、最安全 | 需要额外依赖 | ⭐⭐⭐⭐ 可选 |
| PBKDF2 | 标准算法 | 需要手动管理salt | ⭐⭐⭐ 可用 |
| SCrypt | 内存密集型 | 需要额外依赖 | ⭐⭐⭐ 可用 |
| MD5/SHA | 速度快 | 不安全，不推荐 | ❌ 不推荐 |

## 实现方案

### 1. 添加依赖

Spring Boot 3.x 已包含Spring Security，无需额外添加依赖。

如果需要单独使用BCrypt，可以添加：

```gradle
implementation 'org.springframework.security:spring-security-crypto'
```

### 2. 密码加密工具类

创建 `PasswordEncoderUtil` 工具类，封装BCrypt操作。

### 3. 使用示例

#### 注册/创建用户时加密密码
```java
String rawPassword = "123456";
String encodedPassword = PasswordEncoderUtil.encode(rawPassword);
// 存储 encodedPassword 到数据库
```

#### 登录时验证密码
```java
String rawPassword = "123456"; // 用户输入的密码
String encodedPassword = user.getPassword(); // 数据库中的加密密码
boolean matches = PasswordEncoderUtil.matches(rawPassword, encodedPassword);
```

### 4. 配置建议

- **强度（strength）**：推荐使用 10-12
  - 10：适合大多数应用（默认）
  - 12：更高安全性，但计算稍慢
  - 不建议超过12，会影响性能

- **密码策略**
  - 最小长度：8-12位
  - 包含大小写字母、数字、特殊字符
  - 定期更换密码

## 安全最佳实践

1. **永远不要存储明文密码**
2. **使用BCrypt等单向哈希算法**（不是加密，因为不需要解密）
3. **每次加密都生成新的哈希值**（BCrypt自动处理）
4. **验证时使用matches方法**（不要自己比较字符串）
5. **密码传输使用HTTPS**
6. **实施密码强度策略**
7. **记录登录失败次数，防止暴力破解**

## 迁移现有密码

如果已有明文密码，需要：
1. 批量加密现有密码
2. 用户首次登录时强制修改密码
3. 逐步迁移

## 示例输出

BCrypt加密后的密码格式：
```
$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
```

格式说明：
- `$2a$` - BCrypt版本标识
- `10` - 强度（2^10 = 1024轮）
- 后面是盐值和哈希值（共60个字符）

## 性能参考

- **强度10**：每次加密约100-200ms
- **强度12**：每次加密约400-800ms
- 验证速度与加密速度相近

对于登录验证，这个延迟是可以接受的，还能有效防止暴力破解。

