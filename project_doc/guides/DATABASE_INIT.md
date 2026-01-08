# 数据库初始化说明

## 数据库信息

- **数据库名**：`camp`
- **服务器地址**：`150.158.115.149:3306`
- **用户名**：`campuser`
- **密码**：`camp123456`
- **时区**：北京时间（Asia/Shanghai）

## 初始化方法

### 方法一：使用Shell脚本（推荐）

如果系统已安装MySQL客户端，可以直接运行初始化脚本：

```bash
# 给脚本添加执行权限
chmod +x init_database.sh

# 执行初始化脚本
./init_database.sh
```

脚本会自动：
1. 创建 `camp` 数据库（如果不存在）
2. 执行 `src/main/resources/db/schema.sql` 中的所有建表语句
3. 初始化基础数据（角色、权限等）

### 方法二：使用MySQL客户端命令行

```bash
# 1. 创建数据库（需要使用root用户或有CREATE权限的用户）
mysql -h 150.158.115.149 -P 3306 -u root -p \
  -e "CREATE DATABASE IF NOT EXISTS camp DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;"

# 2. 执行建表语句（使用campuser用户）
mysql -h 150.158.115.149 -P 3306 -u campuser -pcamp123456 camp \
  < src/main/resources/db/schema.sql
```

### 方法三：使用MySQL Workbench或其他GUI工具

1. 连接到MySQL服务器：
   - Host: `150.158.115.149`
   - Port: `3306`
   - Username: `campuser`
   - Password: `camp123456`

2. 创建数据库：
   ```sql
   CREATE DATABASE IF NOT EXISTS camp 
     DEFAULT CHARACTER SET utf8mb4 
     DEFAULT COLLATE utf8mb4_unicode_ci;
   ```

3. 设置时区：
   ```sql
   SET time_zone = '+08:00';
   ```

4. 选择数据库：
   ```sql
   USE camp;
   ```

5. 执行SQL脚本：
   - 打开 `src/main/resources/db/schema.sql` 文件
   - 在MySQL Workbench中执行整个脚本

### 方法四：使用应用自动创建（不推荐用于生产环境）

如果配置了 `spring.jpa.hibernate.ddl-auto=update`，应用启动时会自动创建表结构，但不会执行初始化数据。

**注意**：这种方式不会执行 `schema.sql` 中的初始化数据（角色、权限等）。

## 验证初始化结果

### 检查数据库和表

```bash
# 连接MySQL
mysql -h 150.158.115.149 -P 3306 -u campuser -pcamp123456

# 查看数据库
SHOW DATABASES;

# 使用camp数据库
USE camp;

# 查看所有表
SHOW TABLES;

# 查看时区设置
SELECT @@global.time_zone, @@session.time_zone;
```

### 检查初始化数据

```sql
-- 查看角色
SELECT * FROM roles;

-- 查看权限
SELECT * FROM permissions;

-- 查看数据查询权限配置
SELECT * FROM data_query_permissions;
```

应该能看到以下角色：
- ADMIN（系统管理员）
- TEACHER（教师）
- GRADE_ADMIN（年级管理员）
- STUDENT（学生）
- LOGISTICS（后勤人员）

## 时区设置

### 数据库时区

MySQL服务器时区应该设置为北京时间（UTC+8）。可以通过以下方式设置：

```sql
-- 查看当前时区
SELECT @@global.time_zone, @@session.time_zone;

-- 设置会话时区为北京时间
SET time_zone = '+08:00';

-- 设置全局时区（需要SUPER权限）
SET GLOBAL time_zone = '+08:00';
```

### 应用时区

应用配置中已设置时区为北京时间：

```properties
spring.datasource.url=jdbc:mysql://150.158.115.149:3306/camp?serverTimezone=Asia/Shanghai
```

## 常见问题

### 1. 连接失败

**错误**：`Can't connect to MySQL server`

**解决方案**：
- 检查网络连接
- 确认MySQL服务正在运行
- 检查防火墙规则（3306端口是否开放）
- 确认IP地址和端口正确

### 2. 认证失败

**错误**：`Access denied for user 'root'@'...'`

**解决方案**：
- 确认用户名和密码正确
- 检查用户是否有远程连接权限
- 可能需要授权：`GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' IDENTIFIED BY '123456';`

### 3. 字符编码问题

**错误**：中文乱码

**解决方案**：
- 确保数据库使用 `utf8mb4` 字符集
- 确保连接URL包含 `useUnicode=true&characterEncoding=utf8`
- 检查表的字符集：`SHOW CREATE TABLE table_name;`

### 4. 时区问题

**错误**：时间显示不正确

**解决方案**：
- 设置MySQL时区为 `+08:00`
- 确保连接URL包含 `serverTimezone=Asia/Shanghai`
- 检查系统时区设置

### 5. 表已存在错误

**错误**：`Table 'xxx' already exists`

**解决方案**：
- 如果表已存在且需要重新创建，先删除表：
  ```sql
  DROP TABLE IF EXISTS table_name;
  ```
- 或者使用 `CREATE TABLE IF NOT EXISTS`（schema.sql已使用）

## 重新初始化

如果需要重新初始化数据库：

```bash
# 删除数据库（谨慎操作！需要DROP权限）
mysql -h 150.158.115.149 -P 3306 -u campuser -pcamp123456 \
  -e "DROP DATABASE IF EXISTS camp;"

# 重新执行初始化脚本
./init_database.sh
```

## 下一步

初始化完成后，可以：

1. **启动应用**：
   ```bash
   ./gradlew bootRun
   ```

2. **测试连接**：
   - 查看应用启动日志，确认数据库连接成功
   - 访问API接口测试功能

3. **查看H2控制台**（如果使用H2）：
   - 访问：http://localhost:8080/h2-console
   - 注意：使用MySQL后，H2控制台已禁用

## 注意事项

1. **生产环境**：建议使用内网地址 `10.0.0.3:3306` 以提高安全性
2. **密码安全**：生产环境应使用强密码
3. **备份**：定期备份数据库
4. **权限控制**：限制数据库用户的权限范围
5. **时区一致性**：确保应用服务器和数据库服务器时区一致

