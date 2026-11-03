-- =============================================================================
-- 二期演示数据（MySQL）
-- 文件：db/seed/phase2_demo.sql
-- 前置：已执行 V1 或 V2，以及 db/seed/phase1_demo.sql。
-- H2 启动不跑本文件，同等语句在 db/h2/seed.sql 末尾。
-- 账号密码仍是 Demo@123456。学生身份证为虚构号码，只用于六要素找回。
-- =============================================================================

UPDATE sys_users
SET id_card = '110101201001011234'
WHERE username = 'student01';

INSERT INTO edu_exams (
  exam_code, exam_name, grade_id, class_id, course_id, subject_name, exam_type,
  start_time, end_time, is_public, status, room_no, remark, created_by
)
SELECT 'EXAM-MID-MATH', '初一1班期中数学', g.id, c.id, co.id, co.course_name, 'MIDTERM',
       '2026-11-20 08:00:00', '2026-11-20 10:00:00', 1, 1, 'A101', '演示：公开期中', t.id
FROM grades g
JOIN classes c ON c.class_code = 'JUNIOR_1_CLASS_1'
JOIN courses co ON co.course_code = 'MATH7'
JOIN sys_users t ON t.username = 'teacher01'
WHERE g.grade_code = 'JUNIOR_1'
ON DUPLICATE KEY UPDATE
  exam_name = VALUES(exam_name),
  grade_id = VALUES(grade_id),
  class_id = VALUES(class_id),
  course_id = VALUES(course_id),
  subject_name = VALUES(subject_name),
  exam_type = VALUES(exam_type),
  start_time = VALUES(start_time),
  end_time = VALUES(end_time),
  is_public = VALUES(is_public),
  status = VALUES(status),
  room_no = VALUES(room_no),
  remark = VALUES(remark),
  created_by = VALUES(created_by);

INSERT INTO edu_exams (
  exam_code, exam_name, grade_id, class_id, course_id, subject_name, exam_type,
  start_time, end_time, is_public, status, room_no, remark, created_by
)
SELECT 'EXAM-FINAL-MATH', '初一年级期末数学', g.id, NULL, co.id, co.course_name, 'FINAL',
       '2027-01-15 08:00:00', '2027-01-15 10:00:00', 1, 1, 'B201', '演示：年级公开期末', u.id
FROM grades g
JOIN courses co ON co.course_code = 'MATH7'
JOIN sys_users u ON u.username = 'academic01'
WHERE g.grade_code = 'JUNIOR_1'
ON DUPLICATE KEY UPDATE
  exam_name = VALUES(exam_name),
  grade_id = VALUES(grade_id),
  class_id = VALUES(class_id),
  course_id = VALUES(course_id),
  subject_name = VALUES(subject_name),
  exam_type = VALUES(exam_type),
  start_time = VALUES(start_time),
  end_time = VALUES(end_time),
  is_public = VALUES(is_public),
  status = VALUES(status),
  room_no = VALUES(room_no),
  remark = VALUES(remark),
  created_by = VALUES(created_by);

INSERT INTO edu_exams (
  exam_code, exam_name, grade_id, class_id, course_id, subject_name, exam_type,
  start_time, end_time, is_public, status, room_no, remark, created_by
)
SELECT 'EXAM-QUIZ-MATH', '初一1班数学测验', g.id, c.id, co.id, co.course_name, 'QUIZ',
       '2026-11-10 14:00:00', '2026-11-10 14:40:00', 0, 2, 'A101', '演示：不公开，学生看不见安排', t.id
FROM grades g
JOIN classes c ON c.class_code = 'JUNIOR_1_CLASS_1'
JOIN courses co ON co.course_code = 'MATH7'
JOIN sys_users t ON t.username = 'teacher01'
WHERE g.grade_code = 'JUNIOR_1'
ON DUPLICATE KEY UPDATE
  exam_name = VALUES(exam_name),
  grade_id = VALUES(grade_id),
  class_id = VALUES(class_id),
  course_id = VALUES(course_id),
  subject_name = VALUES(subject_name),
  exam_type = VALUES(exam_type),
  start_time = VALUES(start_time),
  end_time = VALUES(end_time),
  is_public = VALUES(is_public),
  status = VALUES(status),
  room_no = VALUES(room_no),
  remark = VALUES(remark),
  created_by = VALUES(created_by);

INSERT INTO edu_exam_scores (exam_id, student_id, score, grade_level, recorded_by, recorded_at, remark)
SELECT e.id, s.id, 92.50, 'A', t.id, '2026-11-21 09:00:00', '演示成绩'
FROM edu_exams e
JOIN students s ON s.student_no = 'S20260001'
JOIN sys_users t ON t.username = 'teacher01'
WHERE e.exam_code = 'EXAM-MID-MATH'
ON DUPLICATE KEY UPDATE
  score = VALUES(score),
  grade_level = VALUES(grade_level),
  recorded_by = VALUES(recorded_by),
  recorded_at = VALUES(recorded_at),
  remark = VALUES(remark);

INSERT INTO edu_exam_scores (exam_id, student_id, score, grade_level, recorded_by, recorded_at, remark)
SELECT e.id, s.id, 78.00, 'C', t.id, '2027-01-16 09:00:00', '演示成绩'
FROM edu_exams e
JOIN students s ON s.student_no = 'S20260001'
JOIN sys_users t ON t.username = 'teacher01'
WHERE e.exam_code = 'EXAM-FINAL-MATH'
ON DUPLICATE KEY UPDATE
  score = VALUES(score),
  grade_level = VALUES(grade_level),
  recorded_by = VALUES(recorded_by),
  recorded_at = VALUES(recorded_at),
  remark = VALUES(remark);

INSERT INTO edu_exam_scores (exam_id, student_id, score, grade_level, recorded_by, recorded_at, remark)
SELECT e.id, s.id, 88.00, 'B', t.id, '2026-11-10 16:00:00', '不公开测验的本人成绩'
FROM edu_exams e
JOIN students s ON s.student_no = 'S20260001'
JOIN sys_users t ON t.username = 'teacher01'
WHERE e.exam_code = 'EXAM-QUIZ-MATH'
ON DUPLICATE KEY UPDATE
  score = VALUES(score),
  grade_level = VALUES(grade_level),
  recorded_by = VALUES(recorded_by),
  recorded_at = VALUES(recorded_at),
  remark = VALUES(remark);

INSERT INTO oa_visitors (
  visit_no, visitor_name, id_card, phone, purpose, host_user_id, host_name,
  appointment_time, status, remark, created_by
)
SELECT 'VIS-001', '张敏', '110101199001011234', '13700000001', '家长会沟通', u.id, u.real_name,
       '2026-11-05 09:00:00', 0, '演示：待审', a.id
FROM sys_users u
JOIN sys_users a ON a.username = 'teacher01'
WHERE u.username = 'teacher01'
ON DUPLICATE KEY UPDATE
  visitor_name = VALUES(visitor_name),
  phone = VALUES(phone),
  purpose = VALUES(purpose),
  host_user_id = VALUES(host_user_id),
  host_name = VALUES(host_name),
  appointment_time = VALUES(appointment_time),
  status = VALUES(status),
  remark = VALUES(remark),
  created_by = VALUES(created_by);

INSERT INTO oa_visitors (
  visit_no, visitor_name, id_card, phone, purpose, host_user_id, host_name,
  appointment_time, status, remark, created_by
)
SELECT 'VIS-002', '刘强', '110101198805051234', '13700000002', '教材供应商洽谈', u.id, u.real_name,
       '2026-11-06 14:00:00', 1, '演示：已批准', a.id
FROM sys_users u
JOIN sys_users a ON a.username = 'academic01'
WHERE u.username = 'academic01'
ON DUPLICATE KEY UPDATE
  visitor_name = VALUES(visitor_name),
  phone = VALUES(phone),
  purpose = VALUES(purpose),
  host_user_id = VALUES(host_user_id),
  host_name = VALUES(host_name),
  appointment_time = VALUES(appointment_time),
  status = VALUES(status),
  remark = VALUES(remark),
  created_by = VALUES(created_by);

INSERT INTO oa_visitors (
  visit_no, visitor_name, id_card, phone, purpose, host_user_id, host_name,
  appointment_time, arrival_time, status, remark, created_by
)
SELECT 'VIS-003', '陈静', '110101199212121234', '13700000003', '设备巡检', u.id, u.real_name,
       '2026-11-04 10:00:00', '2026-11-04 10:05:00', 3, '演示：已入校', a.id
FROM sys_users u
JOIN sys_users a ON a.username = 'admin'
WHERE u.username = 'admin'
ON DUPLICATE KEY UPDATE
  visitor_name = VALUES(visitor_name),
  phone = VALUES(phone),
  purpose = VALUES(purpose),
  host_user_id = VALUES(host_user_id),
  host_name = VALUES(host_name),
  appointment_time = VALUES(appointment_time),
  arrival_time = VALUES(arrival_time),
  status = VALUES(status),
  remark = VALUES(remark),
  created_by = VALUES(created_by);

INSERT INTO sys_user_settings (user_id, theme_key)
SELECT id, 'default' FROM sys_users WHERE username = 'admin'
ON DUPLICATE KEY UPDATE theme_key = VALUES(theme_key);

INSERT INTO sys_user_settings (user_id, theme_key)
SELECT id, 'campus-green' FROM sys_users WHERE username = 'student01'
ON DUPLICATE KEY UPDATE theme_key = VALUES(theme_key);
