-- ============================================
-- 简化版本：查询users表100条数据，插入对应的students数据
-- 假设users表中已有足够的学生用户数据
-- ============================================

USE camp;

-- 1. 查询users表中的100条学生数据
SELECT 
    id,
    username,
    real_name,
    user_type,
    status,
    phone,
    email
FROM users
WHERE user_type = 1  -- 1-学生
ORDER BY id
LIMIT 100;

-- 2. 插入对应的学生数据到students表
INSERT INTO students (
    user_id,
    student_no,
    grade,
    class_name,
    major,
    department,
    enrollment_date,
    gender,
    birth_date,
    status,
    created_at,
    updated_at
)
SELECT 
    u.id AS user_id,
    -- 生成学号：年级 + 4位序号（基于ROW_NUMBER）
    CONCAT(
        CASE 
            WHEN u.id % 4 = 0 THEN '2024'
            WHEN u.id % 4 = 1 THEN '2021'
            WHEN u.id % 4 = 2 THEN '2022'
            ELSE '2023'
        END,
        LPAD(
            (SELECT IFNULL(MAX(CAST(SUBSTRING(student_no, 5) AS UNSIGNED)), 0) + 1
             FROM students s2 
             WHERE s2.grade = CASE 
                 WHEN u.id % 4 = 0 THEN '2024'
                 WHEN u.id % 4 = 1 THEN '2021'
                 WHEN u.id % 4 = 2 THEN '2022'
                 ELSE '2023'
             END), 
            4, '0')
    ) AS student_no,
    -- 年级
    CASE 
        WHEN u.id % 4 = 0 THEN '2024'
        WHEN u.id % 4 = 1 THEN '2021'
        WHEN u.id % 4 = 2 THEN '2022'
        ELSE '2023'
    END AS grade,
    -- 班级
    CONCAT('计算机', ((u.id - 1) % 10) + 1, '班') AS class_name,
    -- 专业
    CASE 
        WHEN u.id % 3 = 0 THEN '计算机科学与技术'
        WHEN u.id % 3 = 1 THEN '软件工程'
        ELSE '数据科学与大数据技术'
    END AS major,
    -- 院系
    '计算机学院' AS department,
    -- 入学日期
    CASE 
        WHEN u.id % 4 = 0 THEN '2024-09-01'
        WHEN u.id % 4 = 1 THEN '2021-09-01'
        WHEN u.id % 4 = 2 THEN '2022-09-01'
        ELSE '2023-09-01'
    END AS enrollment_date,
    -- 性别
    (u.id % 2) AS gender,
    -- 出生日期
    DATE_SUB(
        CASE 
            WHEN u.id % 4 = 0 THEN '2024-09-01'
            WHEN u.id % 4 = 1 THEN '2021-09-01'
            WHEN u.id % 4 = 2 THEN '2022-09-01'
            ELSE '2023-09-01'
        END,
        INTERVAL (18 + (u.id % 5)) YEAR
    ) AS birth_date,
    -- 学籍状态
    1 AS status,
    NOW() AS created_at,
    NOW() AS updated_at
FROM users u
WHERE u.user_type = 1
  AND u.id NOT IN (SELECT user_id FROM students WHERE user_id IS NOT NULL)
ORDER BY u.id
LIMIT 100;

-- 3. 验证插入结果
SELECT 
    s.id,
    s.student_no AS 学号,
    u.real_name AS 姓名,
    s.grade AS 年级,
    s.class_name AS 班级,
    s.major AS 专业
FROM students s
INNER JOIN users u ON s.user_id = u.id
ORDER BY s.id DESC
LIMIT 100;

