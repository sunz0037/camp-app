# 从users表插入students数据说明

## 功能说明
从users表中查询100条学生用户数据，并在students表中插入对应的学生信息记录。

## SQL脚本文件

### 1. `query_users_and_insert_students.sql`（推荐）
**用途**：如果users表中已有足够的学生用户数据，直接使用此脚本。

**执行步骤**：
```bash
mysql -h 150.158.115.149 -P 3306 -u campuser -pcamp123456 camp < query_users_and_insert_students.sql
```

**功能**：
- 查询users表中100条学生用户（user_type=1）
- 为每个用户生成对应的学生记录
- 自动生成学号、年级、班级、专业等信息

### 2. `generate_students.sql`（完整版）
**用途**：如果users表中学生数据不足100条，使用此脚本会自动创建测试用户。

**执行步骤**：
```bash
mysql -h 150.158.115.149 -P 3306 -u campuser -pcamp123456 camp < generate_students.sql
```

**功能**：
- 检查users表中学生用户数量
- 如果不足100条，自动创建测试用户
- 插入对应的学生数据

## 数据生成规则

### 学号生成
- 格式：`年级 + 4位序号`
- 示例：`20240001`, `20210001`, `20220001`, `20230001`

### 年级分配
- 根据用户ID取模4分配：
  - `id % 4 = 0` → 2024级
  - `id % 4 = 1` → 2021级
  - `id % 4 = 2` → 2022级
  - `id % 4 = 3` → 2023级

### 班级分配
- 格式：`计算机X班`
- 范围：计算机1班 ~ 计算机10班
- 分配：`(id - 1) % 10 + 1`

### 专业分配
- 根据用户ID取模3分配：
  - `id % 3 = 0` → 计算机科学与技术
  - `id % 3 = 1` → 软件工程
  - `id % 3 = 2` → 数据科学与大数据技术

### 其他字段
- **院系**：计算机学院
- **入学日期**：根据年级设置（2021级：2021-09-01，以此类推）
- **性别**：根据ID取模2（0-女，1-男）
- **出生日期**：入学日期减去18-22年
- **学籍状态**：1-在读

## 执行方法

### 方法一：使用MySQL命令行
```bash
cd /Users/sunz/AIProject/camp-app/src/main/resources/db

# 使用简化版本（推荐）
mysql -h 150.158.115.149 -P 3306 -u campuser -pcamp123456 camp < query_users_and_insert_students.sql

# 或使用完整版本（自动创建用户）
mysql -h 150.158.115.149 -P 3306 -u campuser -pcamp123456 camp < generate_students.sql
```

### 方法二：使用MySQL Workbench
1. 连接到MySQL服务器
2. 选择camp数据库
3. 打开SQL脚本文件
4. 执行整个脚本

### 方法三：在MySQL客户端中执行
```sql
USE camp;
SOURCE /path/to/query_users_and_insert_students.sql;
```

## 验证结果

执行完成后，可以运行以下SQL验证：

```sql
-- 查看插入的学生数据
SELECT 
    s.id,
    s.student_no AS 学号,
    u.real_name AS 姓名,
    u.username AS 用户名,
    s.grade AS 年级,
    s.class_name AS 班级,
    s.major AS 专业,
    s.enrollment_date AS 入学日期
FROM students s
INNER JOIN users u ON s.user_id = u.id
ORDER BY s.id DESC
LIMIT 100;

-- 统计各年级学生数
SELECT 
    grade AS 年级,
    COUNT(*) AS 学生人数
FROM students
GROUP BY grade
ORDER BY grade;

-- 统计各班级学生数
SELECT 
    grade AS 年级,
    class_name AS 班级,
    COUNT(*) AS 学生人数
FROM students
GROUP BY grade, class_name
ORDER BY grade, class_name;

-- 统计各专业学生数
SELECT 
    major AS 专业,
    COUNT(*) AS 学生人数
FROM students
GROUP BY major;
```

## 注意事项

1. **避免重复插入**：脚本中已包含检查，不会重复插入已存在的user_id
2. **学号唯一性**：学号自动生成，确保唯一性
3. **外键约束**：确保users表中存在对应的用户记录
4. **用户类型**：只处理user_type=1（学生）的用户
5. **数据量**：如果users表中学生数据不足100条，实际插入数量会少于100条

## 故障排查

### 问题1：外键约束错误
**错误**：`Cannot add or update a child row: a foreign key constraint fails`

**解决**：确保users表中存在对应的用户记录，且user_id正确。

### 问题2：学号重复
**错误**：`Duplicate entry '20240001' for key 'student_no'`

**解决**：删除已存在的students数据，或修改学号生成逻辑。

### 问题3：插入数量不足100条
**原因**：users表中学生用户数量不足100条

**解决**：使用`generate_students.sql`脚本，会自动创建测试用户。

## 示例输出

执行成功后，应该能看到类似以下的结果：

```
+----+----------+--------+-----------+--------+------------------+--------------+
| id | 学号     | 姓名   | 用户名    | 年级   | 班级            | 专业         |
+----+----------+--------+-----------+--------+------------------+--------------+
|  1 | 20240001 | 学生一 | student0001 | 2024 | 计算机1班      | 计算机科学与技术 |
|  2 | 20210001 | 学生二 | student0002 | 2021 | 计算机2班      | 软件工程     |
|  3 | 20220001 | 学生三 | student0003 | 2022 | 计算机3班      | 数据科学与大数据技术 |
...
+----+----------+--------+-----------+--------+------------------+--------------+
100 rows in set
```

