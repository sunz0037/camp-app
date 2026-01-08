# 学生表结构优化迁移说明

## 概述

本次迁移旨在优化学生表结构，添加班级和年级的外键关联，确保数据一致性和维护逻辑的正确性。

## 维护优先级

按照以下顺序维护数据：

1. **年级信息** (grades) - 最基础，无依赖
2. **班级信息** (classes) - 依赖年级
3. **班级对应授课科目** (class_courses) - 依赖班级和科目
4. **授课科目对应授课教师** - 依赖科目和教师
5. **班级对应学生信息** (students) - 依赖班级

## 迁移步骤

### 步骤1：执行结构修改和数据迁移

```bash
mysql -h 150.158.115.149 -P 3306 -u campuser -pcamp123456 camp < migrate_students_add_foreign_keys.sql
```

此脚本将：
- 添加 `class_id` 和 `grade_id` 字段
- 添加索引
- 根据现有数据自动匹配并填充外键
- 创建触发器自动同步冗余字段

### 步骤2：检查数据迁移结果

执行迁移脚本后，会自动显示统计信息：
- 总学生数
- 成功匹配年级ID的学生数
- 成功匹配班级ID的学生数
- 未匹配的数据数量

### 步骤3：处理未匹配的数据

如果存在未匹配的数据，执行：

```bash
mysql -h 150.158.115.149 -P 3306 -u campuser -pcamp123456 camp < migrate_students_data_cleanup.sql
```

此脚本会：
- 列出所有无法匹配年级的学生
- 列出所有无法匹配班级的学生
- 提供手动修复的SQL示例

### 步骤4：添加外键约束（可选）

在确认所有数据都已正确匹配后，可以取消注释迁移脚本中的外键约束部分：

```sql
ALTER TABLE students 
ADD CONSTRAINT fk_student_class 
    FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE SET NULL,
ADD CONSTRAINT fk_student_grade 
    FOREIGN KEY (grade_id) REFERENCES grades(id) ON DELETE SET NULL;
```

**注意**：如果还有数据未匹配成功，外键约束会失败。

## 数据迁移逻辑

### 年级匹配规则

系统会尝试以下匹配方式：
1. 精确匹配：`students.grade = grades.grade_name`
2. 代码匹配：`students.grade = grades.grade_code`
3. 模糊匹配：`students.grade LIKE '%grades.grade_name%'`
4. 模糊匹配：`students.grade LIKE '%grades.grade_code%'`

### 班级匹配规则

系统会尝试以下匹配方式：
1. 精确匹配班级名称：`students.class_name = classes.class_name`
2. 同时匹配年级：`classes.grade_id = students.grade_id`

## 触发器说明

系统创建了两个触发器来自动同步冗余字段：

1. **trg_student_sync_grade_class_on_insert**：插入时自动同步
2. **trg_student_sync_grade_class_on_update**：更新时自动同步

当 `class_id` 或 `grade_id` 更新时，会自动更新 `grade` 和 `class_name` 字段。

## 回滚方案

如果需要回滚，执行以下SQL：

```sql
-- 删除触发器
DROP TRIGGER IF EXISTS trg_student_sync_grade_class_on_update;
DROP TRIGGER IF EXISTS trg_student_sync_grade_class_on_insert;

-- 删除外键约束（如果已添加）
ALTER TABLE students DROP FOREIGN KEY IF EXISTS fk_student_class;
ALTER TABLE students DROP FOREIGN KEY IF EXISTS fk_student_grade;

-- 删除索引
ALTER TABLE students DROP INDEX IF EXISTS idx_class_id;
ALTER TABLE students DROP INDEX IF EXISTS idx_grade_id;

-- 删除字段
ALTER TABLE students DROP COLUMN IF EXISTS class_id;
ALTER TABLE students DROP COLUMN IF EXISTS grade_id;
```

## 验证

迁移完成后，验证以下内容：

1. 检查数据完整性：
```sql
SELECT 
    COUNT(*) as total,
    COUNT(grade_id) as with_grade_id,
    COUNT(class_id) as with_class_id
FROM students
WHERE status = 1;
```

2. 检查外键关联是否正确：
```sql
SELECT s.id, s.student_no, s.grade, s.class_name, g.grade_name, c.class_name
FROM students s
LEFT JOIN grades g ON s.grade_id = g.id
LEFT JOIN classes c ON s.class_id = c.id
WHERE s.status = 1
LIMIT 10;
```

3. 检查触发器是否正常工作：
```sql
-- 更新一个学生的 class_id，检查 grade 和 class_name 是否自动更新
UPDATE students SET class_id = 1 WHERE id = 1;
SELECT id, grade, class_name, grade_id, class_id FROM students WHERE id = 1;
```

## 注意事项

1. **备份数据**：在执行迁移前，请务必备份数据库
2. **测试环境**：建议先在测试环境执行并验证
3. **数据清理**：迁移前建议清理无效的年级和班级字符串数据
4. **分批执行**：如果数据量很大，可以考虑分批执行迁移

## 后续工作

迁移完成后，需要：

1. 更新Java代码（已完成）
2. 重新编译并部署应用
3. 测试学生管理功能
4. 验证维护顺序是否正确执行

