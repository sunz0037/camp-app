-- ============================================
-- 完整脚本：确保users表有100条学生数据，然后插入对应的students数据
-- ============================================

USE camp;

-- 步骤1：确保users表中有100个学生用户
-- 如果不足，创建测试用户数据

-- 先检查现有学生用户数量
SELECT COUNT(*) AS existing_student_count 
FROM users 
WHERE user_type = 1;

-- 创建不足的学生用户
INSERT INTO users (
    username,
    password,
    real_name,
    user_type,
    status,
    phone,
    email,
    created_at,
    updated_at
)
SELECT 
    CONCAT('student', LPAD(n, 4, '0')) AS username,
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy' AS password,  -- 密码：123456
    CONCAT('学生', 
           CASE 
               WHEN n <= 33 THEN '张'
               WHEN n <= 66 THEN '李'
               ELSE '王'
           END,
           CASE 
               WHEN n % 10 = 0 THEN '十'
               WHEN n % 10 = 1 THEN '一'
               WHEN n % 10 = 2 THEN '二'
               WHEN n % 10 = 3 THEN '三'
               WHEN n % 10 = 4 THEN '四'
               WHEN n % 10 = 5 THEN '五'
               WHEN n % 10 = 6 THEN '六'
               WHEN n % 10 = 7 THEN '七'
               WHEN n % 10 = 8 THEN '八'
               ELSE '九'
           END
    ) AS real_name,
    1 AS user_type,  -- 学生
    1 AS status,     -- 启用
    CONCAT('138', LPAD(n, 8, '0')) AS phone,
    CONCAT('student', LPAD(n, 4, '0'), '@example.com') AS email,
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

-- 步骤2：从users表查询100条学生数据，插入到students表

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
    -- 学号：格式为 年级 + 4位序号（例如：20240001）
    CONCAT(
        CASE 
            WHEN u.id % 4 = 0 THEN '2024'
            WHEN u.id % 4 = 1 THEN '2021'
            WHEN u.id % 4 = 2 THEN '2022'
            ELSE '2023'
        END,
        LPAD(ROW_NUMBER() OVER (PARTITION BY 
            CASE 
                WHEN u.id % 4 = 0 THEN '2024'
                WHEN u.id % 4 = 1 THEN '2021'
                WHEN u.id % 4 = 2 THEN '2022'
                ELSE '2023'
            END ORDER BY u.id), 4, '0')
    ) AS student_no,
    -- 年级
    CASE 
        WHEN u.id % 4 = 0 THEN '2024'
        WHEN u.id % 4 = 1 THEN '2021'
        WHEN u.id % 4 = 2 THEN '2022'
        ELSE '2023'
    END AS grade,
    -- 班级：1-10班
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
    -- 性别：随机分配
    (u.id % 2) AS gender,
    -- 出生日期：18-22岁之间
    DATE_SUB(
        CASE 
            WHEN u.id % 4 = 0 THEN '2024-09-01'
            WHEN u.id % 4 = 1 THEN '2021-09-01'
            WHEN u.id % 4 = 2 THEN '2022-09-01'
            ELSE '2023-09-01'
        END,
        INTERVAL (18 + (u.id % 5)) YEAR
    ) AS birth_date,
    -- 学籍状态：1-在读
    1 AS status,
    NOW() AS created_at,
    NOW() AS updated_at
FROM users u
WHERE u.user_type = 1  -- 只处理学生类型的用户
  AND u.id NOT IN (SELECT user_id FROM students WHERE user_id IS NOT NULL)  -- 避免重复插入
ORDER BY u.id
LIMIT 100;

-- 步骤3：验证插入结果

-- 查看插入的学生数据统计
SELECT 
    grade AS 年级,
    class_name AS 班级,
    major AS 专业,
    COUNT(*) AS 学生人数
FROM students
GROUP BY grade, class_name, major
ORDER BY grade, class_name;

-- 查看前10条学生数据详情
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
ORDER BY s.id
LIMIT 10;

-- 统计总数
SELECT 
    COUNT(*) AS 学生总数,
    COUNT(DISTINCT grade) AS 年级数,
    COUNT(DISTINCT class_name) AS 班级数,
    COUNT(DISTINCT major) AS 专业数
FROM students;

