-- ============================================
-- 简化版测试数据插入脚本
-- 直接指定user_id，避免使用子查询
-- ============================================

-- ============================================
-- 1. 插入用户数据（users表）
-- ============================================

-- 学生用户（user_type = 1）
INSERT INTO users (username, password, real_name, id_card, phone, email, user_type, status, created_at, updated_at) VALUES
('20240001', '$2a$05$placeholder', '张三', '110101200001011234', '13800138001', 'zhangsan@example.com', 1, 1, NOW(), NOW()),
('20240002', '$2a$05$placeholder', '李四', '110101200002021234', '13800138002', 'lisi@example.com', 1, 1, NOW(), NOW()),
('20240003', '$2a$05$placeholder', '王五', '110101200003031234', '13800138003', 'wangwu@example.com', 1, 1, NOW(), NOW()),
('20240004', '$2a$05$placeholder', '赵六', '110101200004041234', '13800138004', 'zhaoliu@example.com', 1, 1, NOW(), NOW()),
('20240005', '$2a$05$placeholder', '钱七', '110101200005051234', '13800138005', 'qianqi@example.com', 1, 1, NOW(), NOW()),
('20240006', '$2a$05$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '孙八', '110101200006061234', '13800138006', 'sunba@example.com', 1, 1, NOW(), NOW()),
('20240007', '$2a$05$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '周九', '110101200007071234', '13800138007', 'zhoujiu@example.com', 1, 1, NOW(), NOW()),
('20240008', '$2a$05$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '吴十', '110101200008081234', '13800138008', 'wushi@example.com', 1, 1, NOW(), NOW()),
('20240009', '$2a$05$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '郑十一', '110101200009091234', '13800138009', 'zhengshiyi@example.com', 1, 1, NOW(), NOW()),
('20240010', '$2a$05$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '王十二', '110101200010101234', '13800138010', 'wangshier@example.com', 1, 1, NOW(), NOW());

-- 教工用户（user_type = 2）
INSERT INTO users (username, password, real_name, id_card, phone, email, user_type, status, created_at, updated_at) VALUES
('T001', '$2a$05$placeholder', '李老师', '110101198001011234', '13900139001', 'lilaoshi@example.com', 2, 1, NOW(), NOW()),
('T002', '$2a$05$placeholder', '王老师', '110101198002021234', '13900139002', 'wanglaoshi@example.com', 2, 1, NOW(), NOW()),
('T003', '$2a$05$placeholder', '张老师', '110101198003031234', '13900139003', 'zhanglaoshi@example.com', 2, 1, NOW(), NOW()),
('T004', '$2a$05$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '刘老师', '110101198004041234', '13900139004', 'liulaoshi@example.com', 2, 1, NOW(), NOW()),
('T005', '$2a$05$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '陈老师', '110101198005051234', '13900139005', 'chenlaoshi@example.com', 2, 1, NOW(), NOW());

-- 管理员用户（user_type = 3）
INSERT INTO users (username, password, real_name, id_card, phone, email, user_type, status, created_at, updated_at) VALUES
('admin', '$2a$05$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '系统管理员', '110101198000011234', '13700137001', 'admin@example.com', 3, 1, NOW(), NOW());

-- ============================================
-- 2. 插入学生数据（students表）
-- ============================================
-- 注意：需要先执行users表的插入，然后获取对应的user_id
-- 这里使用子查询方式，根据username匹配

INSERT INTO students (user_id, student_no, grade, class_name, major, department, enrollment_date, dormitory, address, political_status, nationality, gender, birth_date, status, created_at, updated_at)
SELECT 
    u.id,
    u.username,
    '2024级' AS grade,
    CASE 
        WHEN MOD(CAST(SUBSTRING(u.username, 5) AS UNSIGNED), 2) = 0 THEN '计算机1班'
        ELSE '计算机2班'
    END AS class_name,
    '计算机科学与技术' AS major,
    '信息工程学院' AS department,
    '2024-09-01' AS enrollment_date,
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
    1 AS status,
    NOW() AS created_at,
    NOW() AS updated_at
FROM users u
WHERE u.user_type = 1 AND u.username LIKE '2024%'
ORDER BY u.username;

-- ============================================
-- 3. 测试账号说明
-- ============================================
-- 
-- 首次登录用户（需要激活）：
-- 学生：
--   学号: 20240001, 姓名: 张三, 密码: 未设置（需激活）
--   学号: 20240002, 姓名: 李四, 密码: 未设置（需激活）
--   学号: 20240003, 姓名: 王五, 密码: 未设置（需激活）
--   学号: 20240004, 姓名: 赵六, 密码: 未设置（需激活）
--   学号: 20240005, 姓名: 钱七, 密码: 未设置（需激活）
-- 
-- 教工：
--   工号: T001, 姓名: 李老师, 密码: 未设置（需激活）
--   工号: T002, 姓名: 王老师, 密码: 未设置（需激活）
--   工号: T003, 姓名: 张老师, 密码: 未设置（需激活）
-- 
-- 已激活用户（可直接登录）：
-- 学生：
--   学号: 20240006, 姓名: 孙八, 密码: 123456
--   学号: 20240007, 姓名: 周九, 密码: 123456
--   学号: 20240008, 姓名: 吴十, 密码: 123456
--   学号: 20240009, 姓名: 郑十一, 密码: 123456
--   学号: 20240010, 姓名: 王十二, 密码: 123456
-- 
-- 教工：
--   工号: T004, 姓名: 刘老师, 密码: 123456
--   工号: T005, 姓名: 陈老师, 密码: 123456
-- 
-- 管理员：
--   用户名: admin, 姓名: 系统管理员, 密码: 123456
-- 
-- ============================================
-- 4. 密码说明
-- ============================================
-- 占位符密码 '$2a$05$placeholder' 表示首次登录，需要通过激活流程设置密码
-- 已加密密码 '$2a$05$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy' 
--   对应明文密码: 123456（BCrypt强度5）
-- 
-- 如果需要生成新的BCrypt密码，可以使用：
-- 1. Java代码: PasswordEncoderUtil.encode("your_password")
-- 2. 在线工具: https://bcrypt-generator.com/
-- 3. 后端激活接口自动生成

