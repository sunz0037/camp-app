-- 首期演示数据。开发态默认用户：1 管理员 / 2 教务 / 3 教师 / 4 学生。
-- 密码认证后置。password 仅为占位，联调请用 Bearer dev-角色。

INSERT INTO roles (id, role_code, role_name, description, status) VALUES
(1, 'ADMIN', '系统管理员', '首期四档', 1),
(2, 'ACADEMIC', '教务', '授权年级', 1),
(3, 'TEACHER', '教师', '授权班级', 1),
(4, 'STUDENT', '学生', '家长同权，无独立家长角色', 1)
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

INSERT INTO users (id, username, password, real_name, phone, user_type, status, must_change_password) VALUES
(1, 'admin', 'DEFAULT_PASSWORD_NOT_SET', '系统管理员', '13800000001', 3, 1, 0),
(2, 'academic', 'DEFAULT_PASSWORD_NOT_SET', '教务老师', '13800000002', 2, 1, 0),
(3, 'teacher', 'DEFAULT_PASSWORD_NOT_SET', '班主任', '13800000003', 2, 1, 0),
(4, 'student', 'DEFAULT_PASSWORD_NOT_SET', '张三', '13800000004', 1, 1, 0)
ON DUPLICATE KEY UPDATE real_name = VALUES(real_name);

INSERT INTO user_roles (user_id, role_id)
SELECT 1, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = 1 AND role_id = 1);
INSERT INTO user_roles (user_id, role_id)
SELECT 2, 2 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = 2 AND role_id = 2);
INSERT INTO user_roles (user_id, role_id)
SELECT 3, 3 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = 3 AND role_id = 3);
INSERT INTO user_roles (user_id, role_id)
SELECT 4, 4 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = 4 AND role_id = 4);

INSERT INTO grades (id, grade_code, grade_name, grade_type, grade_level, status) VALUES
(1, 'JUNIOR_1', '初中一年级', 'JUNIOR', 1, 1)
ON DUPLICATE KEY UPDATE grade_name = VALUES(grade_name);

INSERT INTO classes (id, grade_id, class_code, class_name, class_number, capacity, current_count, head_teacher_id, status) VALUES
(1, 1, 'JUNIOR_1_CLASS_1', '初一1班', 1, 50, 1, 3, 1)
ON DUPLICATE KEY UPDATE class_name = VALUES(class_name);

INSERT INTO students (id, user_id, student_no, grade, grade_id, class_name, class_id, gender, status, is_deleted) VALUES
(1, 4, 'S2026001', '初中一年级', 1, '初一1班', 1, 1, 1, 0)
ON DUPLICATE KEY UPDATE class_id = VALUES(class_id);

INSERT INTO emergency_contacts (student_id, contact_name, relationship, phone, is_primary)
SELECT 1, '张父', '父亲', '13900000001', 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM emergency_contacts WHERE student_id = 1 AND phone = '13900000001');

INSERT INTO courses (id, course_code, course_name, credit, course_type, status) VALUES
(1, 'MATH', '数学', 2.0, '必修', 1)
ON DUPLICATE KEY UPDATE course_name = VALUES(course_name);

INSERT INTO class_courses (id, class_id, course_id, teacher_id, semester, academic_year, status) VALUES
(1, 1, 1, 3, '1', '2025-2026', 1)
ON DUPLICATE KEY UPDATE teacher_id = VALUES(teacher_id);

INSERT INTO class_course_sessions (class_course_id, weekday, period_start, period_end, room_no)
SELECT 1, 1, 1, 2, 'A101' FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM class_course_sessions WHERE class_course_id = 1 AND weekday = 1 AND period_start = 1
);

INSERT INTO sys_teacher_classes (teacher_id, class_id)
SELECT 3, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_teacher_classes WHERE teacher_id = 3 AND class_id = 1);

INSERT INTO sys_affairs_grades (user_id, grade_id)
SELECT 2, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_affairs_grades WHERE user_id = 2 AND grade_id = 1);

INSERT INTO announcements (title, content, publisher_id, publish_time, scope_type, status, is_top, is_urgent, view_count) 
SELECT '开学通知', '请于周一前完成课表确认。', 1, NOW(), 'ALL', 1, 1, 0, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM announcements WHERE title = '开学通知');
