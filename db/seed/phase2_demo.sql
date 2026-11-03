-- =============================================================================
-- 二期演示数据（可重复执行）
-- 文件：db/seed/phase2_demo.sql
--
-- 前置：已执行 V1 或 V2，再执行 V3__phase2_schema.sql，然后 db/seed/phase1_demo.sql。
-- 依赖自然键 upsert，不写死自增 ID。
-- 密码策略默认行若已存在（V3 会插入）则不覆盖。
-- 访客按姓名 + 电话 + 预计到达去重，重复执行不改已有访客字段。
-- 手机号、姓名均为虚构演示数据。证件号不写入演示库。
-- =============================================================================

SET NAMES utf8mb4;

SET @demo_password := '$2a$10$GLm2qJBsBYhXGQy6aF9XH.69eYBe39296F2Ez3yVNXQF/m0scw8te';

INSERT INTO sys_password_policy (
  id, min_length, require_upper, require_lower, require_digit, require_special,
  history_size, max_failed, lock_minutes, expire_days
)
SELECT 1, 8, 1, 1, 1, 1, 5, 5, 30, 90
WHERE NOT EXISTS (SELECT 1 FROM sys_password_policy WHERE id = 1);

-- 第二名学生，便于同一场考试有两行成绩。密码仍是 Demo@123456。
INSERT INTO sys_users (username, password, real_name, phone, email, user_type, status)
VALUES ('student02', @demo_password, '学生李小雨', '13800000005', 'student02@demo.example', 1, 1)
ON DUPLICATE KEY UPDATE
  password = VALUES(password),
  real_name = VALUES(real_name),
  phone = VALUES(phone),
  email = VALUES(email),
  user_type = VALUES(user_type),
  status = VALUES(status);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM sys_users u
JOIN roles r ON r.role_code = 'STUDENT'
WHERE u.username = 'student02'
ON DUPLICATE KEY UPDATE user_id = user_id;

INSERT INTO students (
  user_id, student_no, grade_id, class_id, grade, class_name,
  gender, enrollment_date, status, is_deleted
)
SELECT u.id, 'S20260002', g.id, c.id, g.grade_name, c.class_name, 0, '2026-09-01', 1, 0
FROM sys_users u
JOIN grades g ON g.grade_code = 'JUNIOR_1'
JOIN classes c ON c.class_code = 'JUNIOR_1_CLASS_1'
WHERE u.username = 'student02'
ON DUPLICATE KEY UPDATE
  user_id = VALUES(user_id),
  grade_id = VALUES(grade_id),
  class_id = VALUES(class_id),
  grade = VALUES(grade),
  class_name = VALUES(class_name),
  gender = VALUES(gender),
  enrollment_date = VALUES(enrollment_date),
  status = VALUES(status),
  is_deleted = VALUES(is_deleted);

UPDATE classes
SET current_count = 2
WHERE class_code = 'JUNIOR_1_CLASS_1';

-- 演示账号尚未记录改密时间时补一笔。重复执行不把已有时间改掉。
UPDATE sys_users
SET password_changed_at = '2026-09-01 08:00:00',
    failed_login_count = 0,
    must_change_password = 0
WHERE username IN ('admin', 'academic01', 'teacher01', 'student01', 'student02')
  AND password_changed_at IS NULL;

-- 一条历史哈希，内容就是当前演示密码。应用层保留近 N=5 次，这里只造 1 次。
INSERT INTO sys_password_history (user_id, password_hash, created_at)
SELECT u.id, u.password, '2026-09-01 08:00:00'
FROM sys_users u
WHERE u.username = 'student01'
  AND NOT EXISTS (
    SELECT 1 FROM sys_password_history h
    WHERE h.user_id = u.id AND h.password_hash = u.password
  );

INSERT INTO sys_user_preferences (user_id, theme_key, locale, extras)
SELECT u.id, 'campus-blue', 'zh-CN', '{"density":"comfortable"}'
FROM sys_users u
WHERE u.username = 'student01'
ON DUPLICATE KEY UPDATE
  theme_key = VALUES(theme_key),
  locale = VALUES(locale),
  extras = VALUES(extras);

INSERT INTO edu_exams (
  course_id, class_id, title, exam_date, academic_year, semester, created_by
)
SELECT co.id, c.id, '【演示】数学期中测验', '2026-11-10', '2026-2027', 1, t.id
FROM courses co
JOIN classes c ON c.class_code = 'JUNIOR_1_CLASS_1'
JOIN sys_users t ON t.username = 'teacher01'
WHERE co.course_code = 'MATH7'
ON DUPLICATE KEY UPDATE
  academic_year = VALUES(academic_year),
  semester = VALUES(semester),
  created_by = VALUES(created_by);

INSERT INTO edu_exam_scores (exam_id, student_id, score, rank_in_class, remark)
SELECT e.id, s.id, 92.50, 1, NULL
FROM edu_exams e
JOIN classes c ON c.id = e.class_id AND c.class_code = 'JUNIOR_1_CLASS_1'
JOIN courses co ON co.id = e.course_id AND co.course_code = 'MATH7'
JOIN students s ON s.student_no = 'S20260001'
WHERE e.title = '【演示】数学期中测验' AND e.exam_date = '2026-11-10'
ON DUPLICATE KEY UPDATE
  score = VALUES(score),
  rank_in_class = VALUES(rank_in_class),
  remark = VALUES(remark);

INSERT INTO edu_exam_scores (exam_id, student_id, score, rank_in_class, remark)
SELECT e.id, s.id, 78.00, 2, '演示：计算题失分'
FROM edu_exams e
JOIN classes c ON c.id = e.class_id AND c.class_code = 'JUNIOR_1_CLASS_1'
JOIN courses co ON co.id = e.course_id AND co.course_code = 'MATH7'
JOIN students s ON s.student_no = 'S20260002'
WHERE e.title = '【演示】数学期中测验' AND e.exam_date = '2026-11-10'
ON DUPLICATE KEY UPDATE
  score = VALUES(score),
  rank_in_class = VALUES(rank_in_class),
  remark = VALUES(remark);

INSERT INTO oa_visitors (
  name, phone, org, purpose, host_user_id, host_dept,
  expected_arrive, expected_leave, status, created_by
)
SELECT
  '张来访', '13700000001', '市教育局', '教学视导',
  host.id, '初中部', '2026-11-12 09:00:00', '2026-11-12 11:00:00', 'APPROVED', creator.id
FROM sys_users host
JOIN sys_users creator ON creator.username = 'academic01'
WHERE host.username = 'teacher01'
  AND NOT EXISTS (
    SELECT 1 FROM oa_visitors v
    WHERE v.name = '张来访' AND v.phone = '13700000001' AND v.expected_arrive = '2026-11-12 09:00:00'
  );

INSERT INTO oa_visitors (
  name, phone, purpose, host_user_id, host_dept,
  expected_arrive, expected_leave, status, created_by
)
SELECT
  '陈家长', '13700000002', '参加家长会',
  host.id, '初中部', '2026-11-15 14:00:00', '2026-11-15 16:00:00', 'PENDING', creator.id
FROM sys_users host
JOIN sys_users creator ON creator.username = 'teacher01'
WHERE host.username = 'teacher01'
  AND NOT EXISTS (
    SELECT 1 FROM oa_visitors v
    WHERE v.name = '陈家长' AND v.phone = '13700000002' AND v.expected_arrive = '2026-11-15 14:00:00'
  );

INSERT INTO oa_visitor_logs (visitor_id, action, operator_id, note, created_at)
SELECT v.id, 'APPROVE', u.id, '演示：教务批准视导', '2026-11-11 10:00:00'
FROM oa_visitors v
JOIN sys_users u ON u.username = 'academic01'
WHERE v.name = '张来访' AND v.phone = '13700000001' AND v.expected_arrive = '2026-11-12 09:00:00'
  AND NOT EXISTS (
    SELECT 1 FROM oa_visitor_logs l
    WHERE l.visitor_id = v.id AND l.action = 'APPROVE'
  );

INSERT INTO oa_visitor_logs (visitor_id, action, operator_id, note, created_at)
SELECT v.id, 'SUBMIT', u.id, '演示：班主任提交预约', '2026-11-14 09:00:00'
FROM oa_visitors v
JOIN sys_users u ON u.username = 'teacher01'
WHERE v.name = '陈家长' AND v.phone = '13700000002' AND v.expected_arrive = '2026-11-15 14:00:00'
  AND NOT EXISTS (
    SELECT 1 FROM oa_visitor_logs l
    WHERE l.visitor_id = v.id AND l.action = 'SUBMIT'
  );

INSERT INTO oa_files (owner_id, parent_id, name, storage_key, mime, size_bytes, is_dir)
SELECT u.id, NULL, '数学资料', 'dir/teacher01/math', NULL, 0, 1
FROM sys_users u
WHERE u.username = 'teacher01'
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  parent_id = VALUES(parent_id),
  mime = VALUES(mime),
  size_bytes = VALUES(size_bytes),
  is_dir = VALUES(is_dir),
  deleted_at = NULL;

INSERT INTO oa_files (owner_id, parent_id, name, storage_key, mime, size_bytes, is_dir)
SELECT u.id, p.id, '期中测验说明.txt', 'obj/teacher01/math/midterm-note.txt', 'text/plain', 128, 0
FROM sys_users u
JOIN oa_files p ON p.storage_key = 'dir/teacher01/math'
WHERE u.username = 'teacher01'
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  parent_id = VALUES(parent_id),
  mime = VALUES(mime),
  size_bytes = VALUES(size_bytes),
  is_dir = VALUES(is_dir),
  deleted_at = NULL;

INSERT INTO oa_files (owner_id, parent_id, name, storage_key, mime, size_bytes, is_dir)
SELECT u.id, p.id, '成绩草表.csv', 'obj/teacher01/math/score-draft.csv', 'text/csv', 64, 0
FROM sys_users u
JOIN oa_files p ON p.storage_key = 'dir/teacher01/math'
WHERE u.username = 'teacher01'
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  parent_id = VALUES(parent_id),
  mime = VALUES(mime),
  size_bytes = VALUES(size_bytes),
  is_dir = VALUES(is_dir),
  deleted_at = NULL;

INSERT INTO oa_file_shares (file_id, share_to_user_id, share_to_role_id, permission, expires_at)
SELECT f.id, u.id, NULL, 'READ', '2027-06-30 23:59:59'
FROM oa_files f
JOIN sys_users u ON u.username = 'student01'
WHERE f.storage_key = 'obj/teacher01/math/midterm-note.txt'
ON DUPLICATE KEY UPDATE
  permission = VALUES(permission),
  expires_at = VALUES(expires_at);
