# 数据库配置说明

## MySQL 数据库配置

### 连接信息
- **公网地址**：`150.158.115.149:3306`
- **内网地址**：`10.0.0.3:3306`
- **用户名**：`campuser`
- **密码**：`camp123456`
- **数据库名**：`camp`（需要手动创建）

### 当前配置
应用当前配置为使用**公网地址**连接MySQL数据库。

如需切换到内网地址，请修改 `application.properties` 文件：
```properties
# 使用内网地址（注释掉公网地址，启用内网地址）
# spring.datasource.url=jdbc:mysql://150.158.115.149:3306/campus_oa?...
spring.datasource.url=jdbc:mysql://10.0.0.3:3306/campus_oa?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
```

## 数据库初始化

### 1. 创建数据库
在MySQL中执行以下SQL创建数据库：
```sql
CREATE DATABASE IF NOT EXISTS campus_oa 
  DEFAULT CHARACTER SET utf8mb4 
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

### 2. 初始化表结构
有两种方式初始化表结构：

#### 方式一：使用JPA自动创建（推荐）
当前配置已启用JPA自动创建表：
```properties
spring.jpa.hibernate.ddl-auto=update
```
应用启动时会自动根据Entity类创建或更新表结构。

#### 方式二：手动执行SQL脚本
如果需要使用预定义的SQL脚本，可以：
1. 取消注释 `application.properties` 中的SQL初始化配置：
```properties
spring.sql.init.mode=always
spring.sql.init.schema-locations=classpath:db/schema.sql
spring.sql.init.continue-on-error=true
```
2. 将 `spring.jpa.hibernate.ddl-auto` 设置为 `none` 或 `validate`

### 3. 初始化数据
如果需要初始化基础数据（角色、权限等），可以：
1. 查看 `src/main/resources/db/schema.sql` 文件中的初始化数据
2. 手动执行SQL或启用SQL脚本初始化

## 连接测试

### 使用MySQL客户端测试连接
```bash
# 使用公网地址
mysql -h 150.158.115.149 -P 3306 -u campuser -pcamp123456

# 使用内网地址
mysql -h 10.0.0.3 -P 3306 -u campuser -pcamp123456
```

### 测试应用连接
启动应用后，检查日志中是否有数据库连接错误。如果连接成功，会看到类似以下日志：
```
HikariPool-1 - Starting...
HikariPool-1 - Start completed.
```

## H2 数据库配置（已注释）

H2数据库配置已注释，如需切换回H2，请：
1. 在 `build.gradle` 中取消注释H2依赖，注释MySQL依赖
2. 在 `application.properties` 中取消注释H2配置，注释MySQL配置

## 注意事项

1. **数据库必须存在**：应用启动前需要先创建 `campus_oa` 数据库
2. **网络连接**：确保应用服务器能够访问MySQL服务器（公网或内网）
3. **防火墙**：确保MySQL服务器的3306端口已开放
4. **字符编码**：数据库和表应使用 `utf8mb4` 字符集
5. **时区设置**：连接URL中已设置 `serverTimezone=Asia/Shanghai`

## 故障排查

### 连接失败
- 检查网络连接是否正常
- 检查MySQL服务是否运行
- 检查防火墙规则
- 检查用户名密码是否正确（用户名：campuser，密码：camp123456）
- 检查用户是否有远程连接权限

### 表创建失败
- 检查数据库用户是否有CREATE权限
- 检查字符集设置是否正确
- 查看应用日志中的详细错误信息

### 字符编码问题
- 确保数据库使用 `utf8mb4` 字符集
- 确保连接URL中包含 `useUnicode=true&characterEncoding=utf8`

