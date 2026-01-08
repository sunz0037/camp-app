-- ============================================
-- 测试数据插入脚本
-- 用于插入users和students表的数据
-- ============================================
-- 注意：密码字段需要先使用BCrypt加密，本脚本中的密码为明文示例
-- 实际使用时，需要先加密密码再插入
-- ============================================

-- 清空现有测试数据（可选，谨慎使用）
-- DELETE FROM students WHERE user_id IN (SELECT id FROM users WHERE username LIKE 'test%');
-- DELETE FROM users WHERE username LIKE 'test%';

-- ============================================
-- 1. 插入用户数据（users表）
-- ============================================

-- 学生用户（user_type = 1）
INSERT INTO users (username, password, real_name, id_card, phone, email, user_type, status, avatar_url, created_at, updated_at) VALUES
-- 学生1-5（首次登录，密码未设置，使用占位符）
('20240001', '$2a$05$placeholder', '张三', '110101200001011234', '13800138001', 'zhangsan@example.com', 1, 1, NULL, NOW(), NOW()),
('20240002', '$2a$05$placeholder', '李四', '110101200002021234', '13800138002', 'lisi@example.com', 1, 1, NULL, NOW(), NOW()),
('20240003', '$2a$05$placeholder', '王五', '110101200003031234', '13800138003', 'wangwu@example.com', 1, 1, NULL, NOW(), NOW()),
('20240004', '$2a$05$placeholder', '赵六', '110101200004041234', '13800138004', 'zhaoliu@example.com', 1, 1, NULL, NOW(), NOW()),
('20240005', '$2a$05$placeholder', '钱七', '110101200005051234', '13800138005', 'qianqi@example.com', 1, 1, NULL, NOW(), NOW()),

-- 学生6-10（已激活，密码：123456）
('20240006', '$2a$05$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '孙八', '110101200006061234', '13800138006', 'sunba@example.com', 1, 1, NULL, NOW(), NOW()),
('20240007', '$2a$05$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '周九', '110101200007071234', '13800138007', 'zhoujiu@example.com', 1, 1, NULL, NOW(), NOW()),
('20240008', '$2a$05$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '吴十', '110101200008081234', '13800138008', 'wushi@example.com', 1, 1, NULL, NOW(), NOW()),
('20240009', '$2a$05$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '郑十一', '110101200009091234', '13800138009', 'zhengshiyi@example.com', 1, 1, NULL, NOW(), NOW()),
('20240010', '$2a$05$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '王十二', '110101200010101234', '13800138010', 'wangshier@example.com', 1, 1, NULL, NOW(), NOW()),

-- 教工用户（user_type = 2）
-- 教工1-3（首次登录，密码未设置）
('T001', '$2a$05$placeholder', '李老师', '110101198001011234', '13900139001', 'lilaoshi@example.com', 2, 1, NULL, NOW(), NOW()),
('T002', '$2a$05$placeholder', '王老师', '110101198002021234', '13900139002', 'wanglaoshi@example.com', 2, 1, NULL, NOW(), NOW()),
('T003', '$2a$05$placeholder', '张老师', '110101198003031234', '13900139003', 'zhanglaoshi@example.com', 2, 1, NULL, NOW(), NOW()),

-- 教工4-5（已激活，密码：123456）
('T004', '$2a$05$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '刘老师', '110101198004041234', '13900139004', 'liulaoshi@example.com', 2, 1, NULL, NOW(), NOW()),
('T005', '$2a$05$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '陈老师', '110101198005051234', '13900139005', 'chenlaoshi@example.com', 2, 1, NULL, NOW(), NOW()),

-- 管理员用户（user_type = 3）
('admin', '$2a$05$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '系统管理员', '110101198000011234', '13700137001', 'admin@example.com', 3, 1, NULL, NOW(), NOW());

-- ============================================
-- 2. 插入学生数据（students表）
-- ============================================
-- 注意：需要先获取users表中对应的user_id
-- 以下SQL假设users表已插入，使用子查询获取user_id

INSERT INTO students (user_id, student_no, grade, class_name, major, department, enrollment_date, graduation_date, dormitory, address, political_status, nationality, gender, birth_date, status, created_at, updated_at)
SELECT 
    u.id AS user_id,
    u.username AS student_no,  -- 使用username作为学号
    CASE 
        WHEN u.username LIKE '2024%' THEN '2024级'
        WHEN u.username LIKE '2023%' THEN '2023级'
        WHEN u.username LIKE '2022%' THEN '2022级'
        ELSE '2024级'
    END AS grade,
    CASE 
        WHEN MOD(CAST(SUBSTRING(u.username, 5) AS UNSIGNED), 2) = 0 THEN '计算机1班'
        ELSE '计算机2班'
    END AS class_name,
    '计算机科学与技术' AS major,
    '信息工程学院' AS department,
    '2024-09-01' AS enrollment_date,
    NULL AS graduation_date,
    CONCAT('A', LPAD(MOD(CAST(SUBSTRING(u.username, 5) AS UNSIGNED), 100), 3, '0'), '室') AS dormitory,
    CONCAT('北京市', CASE MOD(CAST(SUBSTRING(u.username, 5) AS UNSIGNED), 10) 
        WHEN 0 THEN '朝阳区'
        WHEN 1 THEN '海淀区'
        WHEN 2 THEN '西城区'
        WHEN 3 THEN '东城区'
        ELSE '丰台区'
    END, '某某街道', CAST(SUBSTRING(u.username, 5) AS UNSIGNED), '号') AS address,
    '共青团员' AS political_status,
    '中国' AS nationality,
    CASE MOD(CAST(SUBSTRING(u.username, 5) AS UNSIGNED), 2) 
        WHEN 0 THEN 1  -- 男
        ELSE 0         -- 女
    END AS gender,
    DATE_SUB('2006-01-01', INTERVAL CAST(SUBSTRING(u.username, 5) AS UNSIGNED) DAY) AS birth_date,
    1 AS status,  -- 在读
    NOW() AS created_at,
    NOW() AS updated_at
FROM users u
WHERE u.user_type = 1  -- 仅学生用户
  AND u.username LIKE '2024%'  -- 仅2024开头的学号
ORDER BY u.username;

-- ============================================
-- 3. 验证数据插入
-- ============================================

-- 查看插入的用户数量
-- SELECT user_type, COUNT(*) as count FROM users GROUP BY user_type;

-- 查看插入的学生数量
-- SELECT COUNT(*) as student_count FROM students;

-- 查看用户和学生关联情况
-- SELECT u.id, u.username, u.real_name, u.user_type, s.student_no, s.grade, s.class_name
-- FROM users u
-- LEFT JOIN students s ON u.id = s.user_id
-- WHERE u.user_type = 1
-- ORDER BY u.username;

-- ============================================
-- 4. 密码说明
-- ============================================
-- 本脚本中的密码处理：
-- 1. 首次登录用户：使用 '$2a$05$placeholder' 作为占位符
--    实际使用时，这些用户需要通过首次登录激活流程设置密码
-- 
-- 2. 已激活用户：使用BCrypt加密后的密码（强度5）
--    示例密码：'123456'
--    加密后：'$2a$05$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'
-- 
-- 3. 如果需要生成新的BCrypt密码，可以使用以下方法：
--    - Java: PasswordEncoderUtil.encode("123456")
--    - 在线工具: https://bcrypt-generator.com/
--    - 后端API: 通过登录激活接口自动生成
-- 
-- ============================================
-- 5. 使用说明
-- ============================================
-- 1. 执行第一部分：插入users表数据
-- 2. 执行第二部分：插入students表数据（会自动关联users表）
-- 3. 验证数据：执行第三部分的查询语句
-- 
-- 注意：
-- - 首次登录的用户（密码为placeholder）需要通过登录页面激活
-- - 已激活的用户可以使用密码 '123456' 登录
-- - 学号格式：20240001-20240010（学生），T001-T005（教工）
-- - 学生姓名：张三、李四、王五等
-- - 教工姓名：李老师、王老师、张老师等

