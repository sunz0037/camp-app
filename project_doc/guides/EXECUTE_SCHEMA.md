# 执行数据库建表语句说明

## 当前状态

由于数据库连接问题（端口3306无法访问），已创建以下工具和文件供您执行建表语句：

## 方法1：使用执行脚本（推荐，如果数据库连接正常）

```bash
cd /Users/sunz/AIProject/camp-app

# 使用公网地址
./execute_schema.sh

# 或使用内网地址
export DB_HOST=10.0.0.3
./execute_schema.sh
```

## 方法2：手动执行SQL文件（如果方法1失败）

### 2.1 使用MySQL客户端

```bash
# 如果已安装MySQL客户端
mysql -h 150.158.115.149 -P 3306 -u campuser -pcamp123456 camp < src/main/resources/db/schema.sql

# 或使用内网地址
mysql -h 10.0.0.3 -P 3306 -u campuser -pcamp123456 camp < src/main/resources/db/schema.sql
```

### 2.2 使用数据库管理工具

1. 使用 Navicat、DBeaver、DataGrip 等工具连接到数据库
2. 连接到 `camp` 数据库
3. 打开 `src/main/resources/db/schema.sql` 文件
4. 执行整个SQL脚本

### 2.3 在MySQL服务器上直接执行

如果您有MySQL服务器的SSH访问权限：

```bash
# SSH连接到MySQL服务器
ssh user@150.158.115.149

# 登录MySQL
mysql -u campuser -pcamp123456 camp

# 在MySQL命令行中执行
source /path/to/schema.sql
# 或
\. /path/to/schema.sql
```

## SQL文件位置

- **建表语句文件**: `src/main/resources/db/schema.sql`
- **包含内容**:
  - 16个数据表的创建语句
  - 基础角色和权限的初始化数据
  - 数据查询权限配置

## 执行后的验证

执行完成后，可以验证表是否创建成功：

```sql
-- 查看所有表
SHOW TABLES;

-- 应该看到以下表：
-- users, students, emergency_contacts, courses, exam_scores,
-- announcements, visitor_registrations, roles, permissions,
-- role_permissions, user_roles, teacher_classes, grade_admin_grades,
-- data_query_permissions, class_attendance_daily, student_attendance_records

-- 查看角色数据
SELECT * FROM roles;

-- 应该看到：ADMIN, TEACHER, GRADE_ADMIN, STUDENT, LOGISTICS
```

## 注意事项

1. **表已存在**: 如果表已存在，`CREATE TABLE IF NOT EXISTS` 会跳过创建，不会报错
2. **数据已存在**: `INSERT ... ON DUPLICATE KEY UPDATE` 会更新已存在的记录
3. **外键约束**: 确保先创建被引用的表（如 `users` 表必须在 `students` 表之前创建）
4. **字符集**: 所有表使用 `utf8mb4` 字符集，支持中文和emoji

## 如果连接仍然失败

请检查：

1. **网络连通性**
   ```bash
   ping 150.158.115.149
   # 或
   ping 10.0.0.3
   ```

2. **端口是否开放**
   ```bash
   nc -zv 150.158.115.149 3306
   # 或
   nc -zv 10.0.0.3 3306
   ```

3. **MySQL服务状态**（在MySQL服务器上）
   ```bash
   systemctl status mysql
   # 或
   systemctl status mysqld
   ```

4. **MySQL配置**（在MySQL服务器上）
   ```bash
   # 检查监听地址
   netstat -tlnp | grep 3306
   # 应该看到 0.0.0.0:3306 或 :::3306
   ```

5. **防火墙规则**（在MySQL服务器上）
   ```bash
   # CentOS/RHEL
   firewall-cmd --list-ports
   
   # Ubuntu/Debian
   ufw status
   ```

## 已创建的工具

1. **ExecuteSchema.java**: Java程序，可以读取并执行schema.sql文件
2. **execute_schema.sh**: Shell脚本，方便执行建表语句
3. **schema.sql**: 完整的建表语句和初始化数据

## 下一步

执行完建表语句后，可以：

1. 启动应用测试连接
2. 插入测试数据（如学生数据）
3. 测试API接口

