-- =============================================================================
-- 首期演示数据（H2，可重复执行）
-- 文件：db/h2/seed.sql
-- 内容与 db/seed/phase1_demo.sql 相同，仅去掉 SET NAMES，并把变量赋值改成 H2 的等号。
-- 账号密码均为 Demo@123456 的 BCrypt 哈希。
--
-- 前置：已执行 db/h2/schema.sql。
-- 依赖自然键 upsert，不写死自增 ID。重复执行会覆盖演示行上的同名字段。
--
-- 演示账号密码均为 Demo@123456（BCrypt 哈希，成本 10）。只保证能对上密码字段，不含激活或重置流程。
-- 手机号、姓名均为虚构演示数据。
-- =============================================================================

-- H2 的 SET 用等号，不用 MySQL 的 :=。
SET @demo_password = '$2a$10$GLm2qJBsBYhXGQy6aF9XH.69eYBe39296F2Ez3yVNXQF/m0scw8te';

INSERT INTO sys_users (username, password, real_name, phone, email, user_type, status)
VALUES
  ('admin', @demo_password, '系统管理员', '13800000001', 'admin@demo.example', 3, 1),
  ('academic01', @demo_password, '教务周宁', '13800000002', 'academic01@demo.example', 2, 1),
  ('teacher01', @demo_password, '班主任李想', '13800000003', 'teacher01@demo.example', 2, 1),
  ('student01', @demo_password, '学生王小明', '13800000004', 'student01@demo.example', 1, 1)
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
JOIN roles r ON r.role_code = 'ADMIN'
WHERE u.username = 'admin'
ON DUPLICATE KEY UPDATE user_id = user_id;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM sys_users u
JOIN roles r ON r.role_code = 'ACADEMIC'
WHERE u.username = 'academic01'
ON DUPLICATE KEY UPDATE user_id = user_id;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM sys_users u
JOIN roles r ON r.role_code = 'TEACHER'
WHERE u.username = 'teacher01'
ON DUPLICATE KEY UPDATE user_id = user_id;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM sys_users u
JOIN roles r ON r.role_code = 'STUDENT'
WHERE u.username = 'student01'
ON DUPLICATE KEY UPDATE user_id = user_id;

INSERT INTO grades (grade_code, grade_name, grade_type, grade_level, description, status)
VALUES
  ('JUNIOR_1', '初中一年级', 'JUNIOR', 1, '演示年级', 1),
  ('JUNIOR_2', '初中二年级', 'JUNIOR', 2, '演示年级，未授权给 academic01', 1)
ON DUPLICATE KEY UPDATE
  grade_name = VALUES(grade_name),
  grade_type = VALUES(grade_type),
  grade_level = VALUES(grade_level),
  description = VALUES(description),
  status = VALUES(status);

INSERT INTO classes (
  grade_id, class_code, class_name, class_number, capacity, current_count, head_teacher_id, description, status
)
SELECT g.id, 'JUNIOR_1_CLASS_1', '初一1班', 1, 50, 1, t.id, '演示班级', 1
FROM grades g
JOIN sys_users t ON t.username = 'teacher01'
WHERE g.grade_code = 'JUNIOR_1'
ON DUPLICATE KEY UPDATE
  class_name = VALUES(class_name),
  grade_id = VALUES(grade_id),
  head_teacher_id = VALUES(head_teacher_id),
  current_count = VALUES(current_count),
  description = VALUES(description),
  status = VALUES(status);

INSERT INTO classes (
  grade_id, class_code, class_name, class_number, capacity, current_count, head_teacher_id, description, status
)
SELECT g.id, 'JUNIOR_1_CLASS_2', '初一2班', 2, 50, 0, NULL, '演示班级，教师未授权', 1
FROM grades g
WHERE g.grade_code = 'JUNIOR_1'
ON DUPLICATE KEY UPDATE
  class_name = VALUES(class_name),
  grade_id = VALUES(grade_id),
  description = VALUES(description),
  status = VALUES(status);

INSERT INTO sys_affairs_grades (user_id, grade_id, grade, description)
SELECT u.id, g.id, g.grade_name, '演示：教务授权初一年级'
FROM sys_users u
JOIN grades g ON g.grade_code = 'JUNIOR_1'
WHERE u.username = 'academic01'
ON DUPLICATE KEY UPDATE
  grade = VALUES(grade),
  description = VALUES(description);

INSERT INTO sys_teacher_classes (teacher_id, class_id, grade, class_name, subject, is_class_teacher)
SELECT u.id, c.id, g.grade_name, c.class_name, '数学', 1
FROM sys_users u
JOIN classes c ON c.class_code = 'JUNIOR_1_CLASS_1'
JOIN grades g ON g.id = c.grade_id
WHERE u.username = 'teacher01'
ON DUPLICATE KEY UPDATE
  grade = VALUES(grade),
  class_name = VALUES(class_name),
  subject = VALUES(subject),
  is_class_teacher = VALUES(is_class_teacher);

INSERT INTO students (
  user_id, student_no, grade_id, class_id, grade, class_name,
  gender, enrollment_date, status, is_deleted
)
SELECT u.id, 'S20260001', g.id, c.id, g.grade_name, c.class_name, 1, '2026-09-01', 1, 0
FROM sys_users u
JOIN grades g ON g.grade_code = 'JUNIOR_1'
JOIN classes c ON c.class_code = 'JUNIOR_1_CLASS_1'
WHERE u.username = 'student01'
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

INSERT INTO emergency_contacts (student_id, contact_name, relationship, phone, is_primary)
SELECT s.id, '王芳', '母亲', '13900000001', 1
FROM students s
WHERE s.student_no = 'S20260001'
  AND NOT EXISTS (
    SELECT 1 FROM emergency_contacts ec
    WHERE ec.student_id = s.id AND ec.contact_name = '王芳' AND ec.phone = '13900000001'
  );

-- H2 不支持 UPDATE ... JOIN，改成子查询。
UPDATE emergency_contacts
SET is_primary = 1, relationship = '母亲'
WHERE contact_name = '王芳'
  AND phone = '13900000001'
  AND student_id IN (SELECT id FROM students WHERE student_no = 'S20260001');

INSERT INTO courses (course_code, course_name, credit, course_type, department, status)
VALUES ('MATH7', '数学', 1.0, '必修', '初中部', 1)
ON DUPLICATE KEY UPDATE
  course_name = VALUES(course_name),
  credit = VALUES(credit),
  course_type = VALUES(course_type),
  department = VALUES(department),
  status = VALUES(status);

INSERT INTO edu_class_courses (class_id, course_id, teacher_id, academic_year, semester, status)
SELECT c.id, co.id, t.id, '2026-2027', 1, 1
FROM classes c
JOIN courses co ON co.course_code = 'MATH7'
JOIN sys_users t ON t.username = 'teacher01'
WHERE c.class_code = 'JUNIOR_1_CLASS_1'
ON DUPLICATE KEY UPDATE
  teacher_id = VALUES(teacher_id),
  status = VALUES(status);

INSERT INTO edu_class_course_sessions (class_course_id, weekday, period_start, period_end, room_no)
SELECT cc.id, 1, 1, 2, 'A101'
FROM edu_class_courses cc
JOIN classes c ON c.id = cc.class_id AND c.class_code = 'JUNIOR_1_CLASS_1'
JOIN courses co ON co.id = cc.course_id AND co.course_code = 'MATH7'
WHERE cc.academic_year = '2026-2027' AND cc.semester = 1
ON DUPLICATE KEY UPDATE room_no = VALUES(room_no);

INSERT INTO edu_class_course_sessions (class_course_id, weekday, period_start, period_end, room_no)
SELECT cc.id, 3, 3, 4, 'A102'
FROM edu_class_courses cc
JOIN classes c ON c.id = cc.class_id AND c.class_code = 'JUNIOR_1_CLASS_1'
JOIN courses co ON co.id = cc.course_id AND co.course_code = 'MATH7'
WHERE cc.academic_year = '2026-2027' AND cc.semester = 1
ON DUPLICATE KEY UPDATE room_no = VALUES(room_no);

INSERT INTO edu_attendance_summaries (
  class_id, attendance_date, grade, class_name,
  expected_count, present_count, absent_count, late_count, early_leave_count, leave_count,
  recorder_id, remark
)
SELECT c.id, '2026-11-03', g.grade_name, c.class_name, 1, 0, 0, 1, 0, 0, t.id, '演示：王小明第一节迟到'
FROM classes c
JOIN grades g ON g.id = c.grade_id
JOIN sys_users t ON t.username = 'teacher01'
WHERE c.class_code = 'JUNIOR_1_CLASS_1'
ON DUPLICATE KEY UPDATE
  grade = VALUES(grade),
  class_name = VALUES(class_name),
  expected_count = VALUES(expected_count),
  present_count = VALUES(present_count),
  absent_count = VALUES(absent_count),
  late_count = VALUES(late_count),
  early_leave_count = VALUES(early_leave_count),
  leave_count = VALUES(leave_count),
  recorder_id = VALUES(recorder_id),
  remark = VALUES(remark);

INSERT INTO edu_attendance_records (
  summary_id, student_id, attendance_date, course_id,
  exception_type, confirm_status, reason_detail, recorder_id
)
SELECT sm.id, st.id, '2026-11-03', co.id, 1, 2, '演示：上午第一节迟到', t.id
FROM edu_attendance_summaries sm
JOIN classes c ON c.id = sm.class_id AND c.class_code = 'JUNIOR_1_CLASS_1'
JOIN students st ON st.student_no = 'S20260001'
JOIN courses co ON co.course_code = 'MATH7'
JOIN sys_users t ON t.username = 'teacher01'
WHERE sm.attendance_date = '2026-11-03'
ON DUPLICATE KEY UPDATE
  summary_id = VALUES(summary_id),
  exception_type = VALUES(exception_type),
  confirm_status = VALUES(confirm_status),
  reason_detail = VALUES(reason_detail),
  recorder_id = VALUES(recorder_id);

INSERT INTO oa_announcements (
  title, content, category, publisher_id, publish_time, is_top, is_urgent, status
)
SELECT
  '【演示】期中教学安排',
  '初一1班请于 2026-11-03 查看数学课表与当日考勤。此为演示公告。',
  '通知',
  u.id,
  '2026-11-03 08:00:00',
  0,
  0,
  1
FROM sys_users u
WHERE u.username = 'academic01'
  AND NOT EXISTS (
    SELECT 1 FROM oa_announcements a WHERE a.title = '【演示】期中教学安排'
  );

INSERT INTO oa_announcement_scopes (announcement_id, scope_type, scope_id)
SELECT a.id, 3, c.id
FROM oa_announcements a
JOIN classes c ON c.class_code = 'JUNIOR_1_CLASS_1'
WHERE a.title = '【演示】期中教学安排'
ON DUPLICATE KEY UPDATE scope_id = VALUES(scope_id);

INSERT INTO oa_announcement_reads (announcement_id, user_id, read_at)
SELECT a.id, u.id, '2026-11-03 08:30:00'
FROM oa_announcements a
JOIN sys_users u ON u.username = 'student01'
WHERE a.title = '【演示】期中教学安排'
ON DUPLICATE KEY UPDATE read_at = VALUES(read_at);
