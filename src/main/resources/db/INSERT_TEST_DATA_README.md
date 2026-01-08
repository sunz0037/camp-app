# 测试数据插入说明

## 文件说明

### 1. insert_test_data.sql
完整版插入脚本，包含详细注释和说明。

### 2. insert_test_data_simple.sql
简化版插入脚本，更简洁易读。

## 数据说明

### 用户数据（users表）

#### 学生用户（10个）
- **学号范围**: 20240001 - 20240010
- **首次登录用户**: 20240001-20240005（密码未设置，需激活）
- **已激活用户**: 20240006-20240010（密码：123456）

#### 教工用户（5个）
- **工号范围**: T001 - T005
- **首次登录用户**: T001-T003（密码未设置，需激活）
- **已激活用户**: T004-T005（密码：123456）

#### 管理员用户（1个）
- **用户名**: admin
- **密码**: 123456（已激活）

### 学生数据（students表）

- **关联**: 通过user_id关联users表
- **学号**: 与users表的username一致
- **年级**: 2024级
- **班级**: 计算机1班、计算机2班（根据学号分配）
- **专业**: 计算机科学与技术
- **院系**: 信息工程学院
- **入学日期**: 2024-09-01
- **其他字段**: 自动生成（宿舍、地址、性别等）

## 使用方法

### 方法1：使用完整版脚本

```sql
-- 1. 连接到数据库
mysql -h 150.158.115.149 -P 3306 -u campuser -pcamp123456 camp

-- 2. 执行插入脚本
source /path/to/insert_test_data.sql;
```

### 方法2：使用简化版脚本

```sql
-- 1. 连接到数据库
mysql -h 150.158.115.149 -P 3306 -u campuser -pcamp123456 camp

-- 2. 执行插入脚本
source /path/to/insert_test_data_simple.sql;
```

### 方法3：使用数据库管理工具

1. 打开Navicat、DBeaver等工具
2. 连接到数据库
3. 打开SQL脚本文件
4. 执行脚本

## 测试账号

### 首次登录测试（需要激活）

**学生**：
- 学号: `20240001`, 姓名: `张三`
- 学号: `20240002`, 姓名: `李四`
- 学号: `20240003`, 姓名: `王五`
- 学号: `20240004`, 姓名: `赵六`
- 学号: `20240005`, 姓名: `钱七`

**教工**：
- 工号: `T001`, 姓名: `李老师`
- 工号: `T002`, 姓名: `王老师`
- 工号: `T003`, 姓名: `张老师`

### 正常登录测试（已激活）

**学生**：
- 学号: `20240006`, 密码: `123456`
- 学号: `20240007`, 密码: `123456`
- 学号: `20240008`, 密码: `123456`
- 学号: `20240009`, 密码: `123456`
- 学号: `20240010`, 密码: `123456`

**教工**：
- 工号: `T004`, 密码: `123456`
- 工号: `T005`, 密码: `123456`

**管理员**：
- 用户名: `admin`, 密码: `123456`

## 密码说明

### 首次登录用户

密码字段使用占位符：`$2a$05$placeholder`

这些用户需要通过登录页面激活：
1. 访问登录页面
2. 输入学号/工号和姓名
3. 系统验证姓名匹配后，弹出密码设置对话框
4. 设置密码（至少6位）
5. 激活成功

### 已激活用户

密码已使用BCrypt加密（强度5）：
- 明文密码: `123456`
- 加密后: `$2a$05$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy`

可以直接使用密码登录。

## 数据验证

### 检查插入的数据

```sql
-- 查看用户数量
SELECT user_type, COUNT(*) as count 
FROM users 
GROUP BY user_type;

-- 查看学生数量
SELECT COUNT(*) as student_count 
FROM students;

-- 查看用户和学生关联
SELECT u.id, u.username, u.real_name, u.user_type, 
       s.student_no, s.grade, s.class_name
FROM users u
LEFT JOIN students s ON u.id = s.user_id
WHERE u.user_type = 1
ORDER BY u.username;

-- 查看首次登录用户
SELECT username, real_name, user_type
FROM users
WHERE password = '$2a$05$placeholder'
ORDER BY user_type, username;
```

## 注意事项

1. **执行顺序**：必须先插入users表，再插入students表
2. **密码加密**：已激活用户的密码已加密，不要直接修改
3. **唯一性**：username和student_no必须唯一
4. **外键约束**：students表的user_id必须对应users表的id
5. **数据清理**：如需重新插入，先删除students表数据，再删除users表数据

## 清理测试数据（谨慎使用）

```sql
-- 删除学生数据
DELETE FROM students WHERE user_id IN (
    SELECT id FROM users WHERE username LIKE '2024%' OR username LIKE 'T%' OR username = 'admin'
);

-- 删除用户数据
DELETE FROM users WHERE username LIKE '2024%' OR username LIKE 'T%' OR username = 'admin';
```

## 扩展数据

如需添加更多测试数据，可以：

1. **复制现有INSERT语句**，修改username、real_name等字段
2. **确保唯一性**：username和student_no不能重复
3. **保持格式一致**：学号格式（2024xxxx），工号格式（Txxx）
4. **密码处理**：首次登录使用占位符，已激活使用BCrypt加密

