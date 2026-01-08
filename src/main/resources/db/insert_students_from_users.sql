-- ============================================
-- 从users表查询100条数据，并在students表插入对应的学生数据
-- ============================================

-- 1. 查询users表中的100条数据（用户类型为学生的，如果没有则查询前100条）
SELECT 
    id,
    username,
    real_name,
    user_type,
    status
FROM users
WHERE user_type = 1  -- 1-学生
ORDER BY id
LIMIT 100;

-- 2. 如果users表中学生数据不足100条，则查询所有用户类型的数据
-- 注意：实际执行时，如果学生数据不足，可能需要先创建用户数据

-- 3. 插入对应的学生数据到students表
-- 使用INSERT INTO ... SELECT语句，基于users表的数据生成学生记录

INSERT INTO students (
    user_id,
    student_no,
    grade,
    class_name,
    major,
    department,
    enrollment_date,
    status,
    created_at,
    updated_at
)
SELECT 
    u.id AS user_id,
    -- 生成学号：格式为 2024 + 4位序号（例如：20240001）
    CONCAT('2024', LPAD(ROW_NUMBER() OVER (ORDER BY u.id), 4, '0')) AS student_no,
    -- 年级：根据用户ID分配（2021-2024）
    CASE 
        WHEN u.id % 4 = 0 THEN '2024'
        WHEN u.id % 4 = 1 THEN '2021'
        WHEN u.id % 4 = 2 THEN '2022'
        ELSE '2023'
    END AS grade,
    -- 班级：根据用户ID分配（1-10班）
    CONCAT('计算机', (u.id % 10) + 1, '班') AS class_name,
    -- 专业：根据用户ID分配
    CASE 
        WHEN u.id % 3 = 0 THEN '计算机科学与技术'
        WHEN u.id % 3 = 1 THEN '软件工程'
        ELSE '数据科学与大数据技术'
    END AS major,
    -- 院系
    '计算机学院' AS department,
    -- 入学日期：根据年级设置
    CASE 
        WHEN u.id % 4 = 0 THEN '2024-09-01'
        WHEN u.id % 4 = 1 THEN '2021-09-01'
        WHEN u.id % 4 = 2 THEN '2022-09-01'
        ELSE '2023-09-01'
    END AS enrollment_date,
    -- 学籍状态：1-在读
    1 AS status,
    NOW() AS created_at,
    NOW() AS updated_at
FROM users u
WHERE u.id NOT IN (SELECT user_id FROM students WHERE user_id IS NOT NULL)  -- 避免重复插入
  AND u.user_type = 1  -- 只处理学生类型的用户
ORDER BY u.id
LIMIT 100;

-- 4. 如果users表中没有足够的用户数据，可以先创建测试用户数据
-- 以下脚本可以创建100个测试用户（如果需要）

-- 创建测试用户（如果users表中数据不足100条）
INSERT INTO users (
    username,
    password,
    real_name,
    user_type,
    status,
    created_at,
    updated_at
)
SELECT 
    CONCAT('student', LPAD(n, 4, '0')) AS username,
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy' AS password,  -- 默认密码：123456（BCrypt加密）
    CONCAT('学生', n) AS real_name,
    1 AS user_type,  -- 学生
    1 AS status,     -- 启用
    NOW() AS created_at,
    NOW() AS updated_at
FROM (
    SELECT @row := @row + 1 AS n
    FROM (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL 
          SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t1,
         (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL 
          SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t2,
         (SELECT @row := 0) r
    LIMIT 100
) numbers
WHERE CONCAT('student', LPAD(n, 4, '0')) NOT IN (SELECT username FROM users)
LIMIT 100;

-- 5. 验证插入结果
-- 查看插入的学生数据
SELECT 
    s.id,
    s.student_no,
    s.grade,
    s.class_name,
    s.major,
    u.real_name,
    u.username
FROM students s
INNER JOIN users u ON s.user_id = u.id
ORDER BY s.id DESC
LIMIT 100;

-- 6. 统计信息
SELECT 
    COUNT(*) AS total_students,
    COUNT(DISTINCT grade) AS total_grades,
    COUNT(DISTINCT class_name) AS total_classes,
    COUNT(DISTINCT major) AS total_majors
FROM students;

