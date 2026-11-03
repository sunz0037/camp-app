-- =============================================================================
-- 旧库对齐到首期结构
-- 文件：db/migration/V2__phase1_legacy_align.sql
--
-- 适用：已经执行过 src/main/resources/db/schema.sql（以及可选的
-- grades_and_classes.sql / class_courses.sql）的库。
-- 空库请执行 V1，不要只跑本文件。
-- 已跑过 V1 的库可以再跑本文件，应当无结构变化。
--
-- 做法：能改名、补列就改，不 DROP 业务表。
-- 成绩 exam_scores、访客 visitor_registrations 只更新表注释为 phase:2，不新增列。
-- 不使用 scope_json / data_query_permissions 作为主授权。
-- =============================================================================

SET NAMES utf8mb4;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name IN ('users', 'sys_users')) = 0,
  'DROP TABLE `__phase1_v2_refuses_to_run_on_empty_database_run_V1_first__`',
  'SELECT ''V2 phase1 legacy align'' AS phase1_step'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 账号底座：旧表 users 改名为 sys_users。不补首次登录、密码重置列。
-- -----------------------------------------------------------------------------
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'users') > 0
  AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'sys_users') = 0,
  'RENAME TABLE users TO sys_users',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_username_uk := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'sys_users' AND column_name = 'username' AND non_unique = 0
);
SET @sql := IF(
  @has_username_uk = 0,
  'ALTER TABLE sys_users ADD UNIQUE KEY uk_username (username)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE sys_users
  MODIFY id_card VARCHAR(18) NULL COMMENT '身份证号。敏感字段：库内可存，列表/导出脱敏由应用层按角色处理，禁止写入审计明文',
  MODIFY phone VARCHAR(20) NULL COMMENT '手机号。敏感字段：库内可存，列表/导出脱敏由应用层按角色处理，禁止写入审计明文',
  MODIFY user_type TINYINT NOT NULL DEFAULT 1 COMMENT '粗分类：1学生 2教职工 3管理员。授权以 roles 为准',
  COMMENT = '用户账号底座';

-- -----------------------------------------------------------------------------
-- 年级 / 班级（旧 schema.sql 未建这两张表）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS grades (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '年级ID',
  grade_code VARCHAR(20) NOT NULL COMMENT '年级代码',
  grade_name VARCHAR(50) NOT NULL COMMENT '年级名称',
  grade_type VARCHAR(20) NOT NULL COMMENT '学段：PRIMARY/JUNIOR/SENIOR',
  grade_level INT NOT NULL COMMENT '学段内序号',
  description VARCHAR(255) NULL COMMENT '说明',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_grade_code (grade_code),
  KEY idx_grade_type (grade_type),
  KEY idx_grade_level (grade_level),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='年级';

CREATE TABLE IF NOT EXISTS classes (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '班级ID',
  grade_id BIGINT NOT NULL COMMENT '年级ID',
  class_code VARCHAR(50) NOT NULL COMMENT '班级代码',
  class_name VARCHAR(50) NOT NULL COMMENT '班级名称',
  class_number INT NOT NULL COMMENT '班号',
  capacity INT NULL DEFAULT 50 COMMENT '容量',
  current_count INT NULL DEFAULT 0 COMMENT '当前人数',
  head_teacher_id BIGINT NULL COMMENT '班主任用户ID',
  description VARCHAR(255) NULL COMMENT '说明',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_class_code (class_code),
  UNIQUE KEY uk_grade_class_number (grade_id, class_number),
  KEY idx_grade_id (grade_id),
  KEY idx_head_teacher_id (head_teacher_id),
  KEY idx_status (status),
  CONSTRAINT fk_classes_grade FOREIGN KEY (grade_id) REFERENCES grades (id),
  CONSTRAINT fk_classes_head_teacher FOREIGN KEY (head_teacher_id) REFERENCES sys_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级';

SET @dup_class_code := (
  SELECT COUNT(*) FROM (
    SELECT class_code FROM classes GROUP BY class_code HAVING COUNT(*) > 1
  ) duplicated_class_codes
);
SET @has_uk_class_code := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'classes' AND index_name = 'uk_class_code'
);
SET @sql := IF(
  @has_uk_class_code = 0 AND @dup_class_code = 0,
  'ALTER TABLE classes ADD UNIQUE KEY uk_class_code (class_code)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 学生、应急联系人
-- -----------------------------------------------------------------------------
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'students' AND column_name = 'grade_id') = 0,
  'ALTER TABLE students ADD COLUMN grade_id BIGINT NULL COMMENT ''年级ID'' AFTER grade',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'students' AND column_name = 'class_id') = 0,
  'ALTER TABLE students ADD COLUMN class_id BIGINT NULL COMMENT ''班级ID'' AFTER class_name',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'students' AND column_name = 'is_deleted') = 0,
  'ALTER TABLE students ADD COLUMN is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT ''软删：0否 1是'' AFTER status',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE students
  MODIFY user_id BIGINT NULL COMMENT '关联用户ID。可空：允许先建学籍再开账号',
  MODIFY status TINYINT NOT NULL DEFAULT 1 COMMENT '学籍：0休学SUSPENDED 1在读ENROLLED 2毕业GRADUATED 3退学WITHDRAWN',
  MODIFY address VARCHAR(255) NULL COMMENT '家庭地址。敏感字段，展示范围由应用层控制';

UPDATE students s
JOIN grades g ON s.grade = g.grade_name OR s.grade = g.grade_code
SET s.grade_id = g.id
WHERE s.grade_id IS NULL AND s.grade IS NOT NULL;

UPDATE students s
JOIN classes c ON c.class_name = s.class_name
JOIN grades g ON g.id = c.grade_id AND g.id = s.grade_id
SET s.class_id = c.id
WHERE s.class_id IS NULL AND s.class_name IS NOT NULL;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'students' AND index_name = 'idx_class_id') = 0,
  'ALTER TABLE students ADD KEY idx_class_id (class_id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'students' AND index_name = 'idx_grade_id') = 0,
  'ALTER TABLE students ADD KEY idx_grade_id (grade_id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE emergency_contacts
  MODIFY phone VARCHAR(20) NOT NULL COMMENT '联系电话。敏感字段：库内可存，列表/导出脱敏由应用层处理',
  MODIFY alternate_phone VARCHAR(20) NULL COMMENT '备用电话。敏感字段，脱敏规则同 phone';

ALTER TABLE courses
  MODIFY teacher_id BIGINT NULL COMMENT '默认教师，可空。排课教师以 edu_class_courses.teacher_id 为准',
  MODIFY semester VARCHAR(20) NULL COMMENT '遗留字段，不是排课来源',
  MODIFY academic_year VARCHAR(20) NULL COMMENT '遗留字段，不是排课来源',
  COMMENT = '课程库。课表见 edu_class_courses + edu_class_course_sessions';

-- -----------------------------------------------------------------------------
-- 排课：class_courses → edu_class_courses，学期改为 tinyint，并补节次表
-- -----------------------------------------------------------------------------
SELECT IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'class_courses') > 0
  AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'edu_class_courses') > 0,
  'WARN: class_courses 与 edu_class_courses 同时存在，跳过改名',
  'OK: class_courses'
) AS align_note;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'class_courses') > 0
  AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'edu_class_courses') = 0,
  'RENAME TABLE class_courses TO edu_class_courses',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS edu_class_courses (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '班级授课ID',
  class_id BIGINT NOT NULL COMMENT '班级ID',
  course_id BIGINT NOT NULL COMMENT '课程ID',
  teacher_id BIGINT NULL COMMENT '授课教师用户ID',
  academic_year VARCHAR(20) NOT NULL COMMENT '学年，如 2026-2027。不用 semester_id',
  semester TINYINT NOT NULL COMMENT '学期：1第一学期 2第二学期',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_class_course_term (class_id, course_id, academic_year, semester),
  KEY idx_class_id (class_id),
  KEY idx_course_id (course_id),
  KEY idx_teacher_id (teacher_id),
  KEY idx_term (academic_year, semester),
  CONSTRAINT chk_edu_class_course_semester CHECK (semester IN (1, 2)),
  CONSTRAINT fk_edu_class_courses_class FOREIGN KEY (class_id) REFERENCES classes (id),
  CONSTRAINT fk_edu_class_courses_course FOREIGN KEY (course_id) REFERENCES courses (id),
  CONSTRAINT fk_edu_class_courses_teacher FOREIGN KEY (teacher_id) REFERENCES sys_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级授课。一学期一门课一行，节次在子表';

SET @semester_is_text := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'edu_class_courses'
    AND column_name = 'semester' AND data_type IN ('varchar', 'char', 'text')
);

SET @sql := IF(
  @semester_is_text > 0
  AND (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'edu_class_courses' AND index_name = 'uk_class_course') > 0,
  'ALTER TABLE edu_class_courses DROP INDEX uk_class_course',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  @semester_is_text > 0
  AND (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'edu_class_courses' AND column_name = 'semester_code') = 0,
  'ALTER TABLE edu_class_courses ADD COLUMN semester_code TINYINT NULL COMMENT ''学期：1第一学期 2第二学期'' AFTER academic_year',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  @semester_is_text > 0,
  'UPDATE edu_class_courses SET semester_code = CASE WHEN semester IN (''1'', ''2'') THEN CAST(semester AS UNSIGNED) WHEN semester REGEXP ''下|二|2'' THEN 2 WHEN semester REGEXP ''上|一|1'' THEN 1 ELSE NULL END WHERE semester_code IS NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  @semester_is_text > 0
  AND (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'edu_class_courses' AND column_name = 'semester_legacy') = 0,
  'ALTER TABLE edu_class_courses CHANGE COLUMN semester semester_legacy VARCHAR(20) NULL COMMENT ''遗留学期字符串，勿再作为排课来源''',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  @semester_is_text > 0
  AND (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'edu_class_courses' AND column_name = 'semester') = 0
  AND (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'edu_class_courses' AND column_name = 'semester_code') > 0,
  'ALTER TABLE edu_class_courses CHANGE COLUMN semester_code semester TINYINT NULL COMMENT ''学期：1第一学期 2第二学期''',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE edu_class_courses SET semester = 1 WHERE semester IS NULL;
UPDATE edu_class_courses SET academic_year = '0000-0000' WHERE academic_year IS NULL OR academic_year = '';

ALTER TABLE edu_class_courses
  MODIFY academic_year VARCHAR(20) NOT NULL COMMENT '学年，如 2026-2027。无法识别的历史值写成 0000-0000，需人工改正。不用 semester_id',
  MODIFY semester TINYINT NOT NULL COMMENT '学期：1第一学期 2第二学期',
  COMMENT = '班级授课。一学期一门课一行，节次在子表';

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'edu_class_courses' AND index_name = 'uk_class_course_term') = 0,
  'ALTER TABLE edu_class_courses ADD UNIQUE KEY uk_class_course_term (class_id, course_id, academic_year, semester)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'edu_class_courses' AND constraint_name = 'chk_edu_class_course_semester') = 0,
  'ALTER TABLE edu_class_courses ADD CONSTRAINT chk_edu_class_course_semester CHECK (semester IN (1, 2))',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS edu_class_course_sessions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '节次ID',
  class_course_id BIGINT NOT NULL COMMENT '班级授课ID',
  weekday TINYINT NOT NULL COMMENT '星期：1周一 … 7周日',
  period_start TINYINT NOT NULL COMMENT '开始节次，从 1 起',
  period_end TINYINT NOT NULL COMMENT '结束节次，含本课',
  room_no VARCHAR(32) NULL COMMENT '教室',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_class_course_session (class_course_id, weekday, period_start, period_end),
  KEY idx_class_course_id (class_course_id),
  KEY idx_weekday_period (weekday, period_start, period_end),
  CONSTRAINT chk_edu_session_slot CHECK (
    weekday BETWEEN 1 AND 7
    AND period_start >= 1
    AND period_end >= period_start
  ),
  CONSTRAINT fk_edu_sessions_class_course FOREIGN KEY (class_course_id) REFERENCES edu_class_courses (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级授课节次';

-- -----------------------------------------------------------------------------
-- 考勤日报
-- -----------------------------------------------------------------------------
SELECT IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'class_attendance_daily') > 0
  AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'edu_attendance_summaries') > 0,
  'WARN: class_attendance_daily 与 edu_attendance_summaries 同时存在，跳过改名',
  'OK: class_attendance_daily'
) AS align_note;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'class_attendance_daily') > 0
  AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'edu_attendance_summaries') = 0,
  'RENAME TABLE class_attendance_daily TO edu_attendance_summaries',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS edu_attendance_summaries (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日报ID',
  class_id BIGINT NOT NULL COMMENT '班级ID',
  attendance_date DATE NOT NULL COMMENT '考勤日期',
  grade VARCHAR(20) NULL COMMENT '年级名称冗余，以 class_id 为准',
  class_name VARCHAR(50) NULL COMMENT '班级名称冗余，以 class_id 为准',
  expected_count INT NOT NULL DEFAULT 0 COMMENT '应到',
  present_count INT NOT NULL DEFAULT 0 COMMENT '实到',
  absent_count INT NOT NULL DEFAULT 0 COMMENT '缺勤人次',
  late_count INT NOT NULL DEFAULT 0 COMMENT '迟到人次',
  early_leave_count INT NOT NULL DEFAULT 0 COMMENT '早退人次',
  leave_count INT NOT NULL DEFAULT 0 COMMENT '请假人次（事假+病假）',
  recorder_id BIGINT NULL COMMENT '记录人用户ID',
  remark VARCHAR(255) NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_class_id_attendance_date (class_id, attendance_date),
  KEY idx_attendance_date (attendance_date),
  KEY idx_recorder_id (recorder_id),
  CONSTRAINT fk_edu_attendance_summaries_class FOREIGN KEY (class_id) REFERENCES classes (id),
  CONSTRAINT fk_edu_attendance_summaries_recorder FOREIGN KEY (recorder_id) REFERENCES sys_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级考勤日报。幂等键 class_id + attendance_date，不挂 course_id';

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'edu_attendance_summaries' AND column_name = 'class_id') = 0,
  'ALTER TABLE edu_attendance_summaries ADD COLUMN class_id BIGINT NULL COMMENT ''班级ID'' AFTER id',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'edu_attendance_summaries' AND column_name = 'early_leave_count') = 0,
  'ALTER TABLE edu_attendance_summaries ADD COLUMN early_leave_count INT NOT NULL DEFAULT 0 COMMENT ''早退人次'' AFTER late_count',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE edu_attendance_summaries s
JOIN classes c ON c.class_name = s.class_name
JOIN grades g ON g.id = c.grade_id AND (s.grade = g.grade_name OR s.grade = g.grade_code OR s.grade IS NULL)
SET s.class_id = c.id
WHERE s.class_id IS NULL AND s.class_name IS NOT NULL;

SET @summary_null_class := (SELECT COUNT(*) FROM edu_attendance_summaries WHERE class_id IS NULL);
SET @sql := IF(
  @summary_null_class = 0,
  'ALTER TABLE edu_attendance_summaries MODIFY class_id BIGINT NOT NULL COMMENT ''班级ID''',
  'SELECT ''WARN: edu_attendance_summaries.class_id 仍有空值，未改为 NOT NULL'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'edu_attendance_summaries' AND index_name = 'uk_class_date' AND column_name = 'grade') > 0,
  'ALTER TABLE edu_attendance_summaries DROP INDEX uk_class_date',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'edu_attendance_summaries' AND index_name = 'uk_class_id_attendance_date') = 0,
  'ALTER TABLE edu_attendance_summaries ADD UNIQUE KEY uk_class_id_attendance_date (class_id, attendance_date)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE edu_attendance_summaries
  COMMENT = '班级考勤日报。幂等键 class_id + attendance_date，不挂 course_id';

-- -----------------------------------------------------------------------------
-- 考勤异常
-- -----------------------------------------------------------------------------
SELECT IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'student_attendance_records') > 0
  AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'edu_attendance_records') > 0,
  'WARN: student_attendance_records 与 edu_attendance_records 同时存在，跳过改名',
  'OK: student_attendance_records'
) AS align_note;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'student_attendance_records') > 0
  AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'edu_attendance_records') = 0,
  'RENAME TABLE student_attendance_records TO edu_attendance_records',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS edu_attendance_records (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '异常ID',
  summary_id BIGINT NULL COMMENT '所属日报，可空以便补录',
  student_id BIGINT NOT NULL COMMENT '学生ID',
  attendance_date DATE NOT NULL COMMENT '考勤日期',
  course_id BIGINT NULL COMMENT '课程ID。空表示当日异常，非空表示某一节课',
  course_key BIGINT AS (IFNULL(course_id, 0)) STORED COMMENT '唯一约束辅助：0 表示未挂课程',
  exception_type TINYINT NOT NULL COMMENT '异常：1 LATE 2 EARLY_LEAVE 3 PERSONAL_LEAVE 4 SICK_LEAVE 5 ABSENT',
  confirm_status TINYINT NOT NULL DEFAULT 2 COMMENT '明细状态：1 PENDING 2 CONFIRMED 3 CLEARED',
  reason_type VARCHAR(30) NULL COMMENT '遗留原因码，首期以 exception_type 为准',
  reason_detail VARCHAR(255) NULL COMMENT '原因说明',
  grade VARCHAR(20) NULL COMMENT '遗留冗余年级',
  class_name VARCHAR(50) NULL COMMENT '遗留冗余班级',
  notified_contact_id BIGINT NULL COMMENT '已通知的应急联系人ID',
  notified_contact_name VARCHAR(50) NULL COMMENT '已通知联系人姓名',
  notified_method VARCHAR(30) NULL COMMENT '通知方式',
  notified_at DATETIME NULL COMMENT '通知时间',
  attachment_url VARCHAR(255) NULL COMMENT '证明材料',
  recorder_id BIGINT NULL COMMENT '记录人用户ID',
  remark VARCHAR(255) NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_student_date_course (student_id, attendance_date, course_key),
  KEY idx_summary_id (summary_id),
  KEY idx_attendance_date (attendance_date),
  KEY idx_exception_type (exception_type),
  KEY idx_course_id (course_id),
  KEY idx_recorder_id (recorder_id),
  CONSTRAINT chk_edu_attendance_exception CHECK (exception_type BETWEEN 1 AND 5),
  CONSTRAINT chk_edu_attendance_confirm CHECK (confirm_status BETWEEN 1 AND 3),
  CONSTRAINT fk_edu_attendance_records_summary FOREIGN KEY (summary_id) REFERENCES edu_attendance_summaries (id),
  CONSTRAINT fk_edu_attendance_records_student FOREIGN KEY (student_id) REFERENCES students (id),
  CONSTRAINT fk_edu_attendance_records_course FOREIGN KEY (course_id) REFERENCES courses (id),
  CONSTRAINT fk_edu_attendance_records_contact FOREIGN KEY (notified_contact_id) REFERENCES emergency_contacts (id),
  CONSTRAINT fk_edu_attendance_records_recorder FOREIGN KEY (recorder_id) REFERENCES sys_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤异常明细。只记异常，不记正常出勤';

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'edu_attendance_records' AND column_name = 'summary_id') = 0,
  'ALTER TABLE edu_attendance_records ADD COLUMN summary_id BIGINT NULL COMMENT ''所属日报，可空以便补录'' AFTER id',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'edu_attendance_records' AND column_name = 'course_id') = 0,
  'ALTER TABLE edu_attendance_records ADD COLUMN course_id BIGINT NULL COMMENT ''课程ID。空表示当日异常，非空表示某一节课'' AFTER attendance_date',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'edu_attendance_records' AND column_name = 'course_key') = 0,
  'ALTER TABLE edu_attendance_records ADD COLUMN course_key BIGINT AS (IFNULL(course_id, 0)) STORED COMMENT ''唯一约束辅助：0 表示未挂课程'' AFTER course_id',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'edu_attendance_records' AND column_name = 'exception_type') = 0,
  'ALTER TABLE edu_attendance_records ADD COLUMN exception_type TINYINT NULL COMMENT ''异常：1 LATE 2 EARLY_LEAVE 3 PERSONAL_LEAVE 4 SICK_LEAVE 5 ABSENT'' AFTER course_key',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'edu_attendance_records' AND column_name = 'confirm_status') = 0,
  'ALTER TABLE edu_attendance_records ADD COLUMN confirm_status TINYINT NOT NULL DEFAULT 2 COMMENT ''明细状态：1 PENDING 2 CONFIRMED 3 CLEARED'' AFTER exception_type',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_legacy_status := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'edu_attendance_records' AND column_name = 'status'
);
SET @sql := IF(
  @has_legacy_status > 0,
  'UPDATE edu_attendance_records SET exception_type = CASE WHEN status = 2 THEN 1 WHEN status = 4 THEN 2 WHEN status = 3 AND reason_type IN (''sick'', ''生病'', ''病假'') THEN 4 WHEN status = 3 THEN 3 WHEN status = 1 THEN 5 ELSE 5 END WHERE exception_type IS NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE edu_attendance_records SET exception_type = 5 WHERE exception_type IS NULL;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'edu_attendance_records' AND index_name = 'idx_status') > 0,
  'ALTER TABLE edu_attendance_records DROP INDEX idx_status',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_legacy_status := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'edu_attendance_records' AND column_name = 'status'
);
SET @sql := IF(
  @has_legacy_status > 0,
  'ALTER TABLE edu_attendance_records DROP COLUMN status',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE edu_attendance_records
  MODIFY exception_type TINYINT NOT NULL COMMENT '异常：1 LATE 2 EARLY_LEAVE 3 PERSONAL_LEAVE 4 SICK_LEAVE 5 ABSENT',
  MODIFY confirm_status TINYINT NOT NULL DEFAULT 2 COMMENT '明细状态：1 PENDING 2 CONFIRMED 3 CLEARED。首期默认 CONFIRMED';

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'edu_attendance_records' AND column_name = 'grade') > 0,
  'ALTER TABLE edu_attendance_records MODIFY grade VARCHAR(20) NULL COMMENT ''遗留冗余年级''',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'edu_attendance_records' AND column_name = 'class_name') > 0,
  'ALTER TABLE edu_attendance_records MODIFY class_name VARCHAR(50) NULL COMMENT ''遗留冗余班级''',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE edu_attendance_records r
JOIN edu_attendance_summaries s
  ON s.attendance_date = r.attendance_date
 AND r.grade IS NOT NULL
 AND r.class_name IS NOT NULL
 AND s.grade = r.grade
 AND s.class_name = r.class_name
SET r.summary_id = s.id
WHERE r.summary_id IS NULL;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'edu_attendance_records' AND index_name = 'uk_student_date') > 0,
  'ALTER TABLE edu_attendance_records DROP INDEX uk_student_date',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'edu_attendance_records' AND index_name = 'uk_student_date_course') = 0,
  'ALTER TABLE edu_attendance_records ADD UNIQUE KEY uk_student_date_course (student_id, attendance_date, course_key)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'edu_attendance_records' AND constraint_name = 'chk_edu_attendance_exception') = 0,
  'ALTER TABLE edu_attendance_records ADD CONSTRAINT chk_edu_attendance_exception CHECK (exception_type BETWEEN 1 AND 5)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'edu_attendance_records' AND constraint_name = 'chk_edu_attendance_confirm') = 0,
  'ALTER TABLE edu_attendance_records ADD CONSTRAINT chk_edu_attendance_confirm CHECK (confirm_status BETWEEN 1 AND 3)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE edu_attendance_records COMMENT = '考勤异常明细。只记异常，不记正常出勤';

-- -----------------------------------------------------------------------------
-- 教师-班级、教务-年级
-- -----------------------------------------------------------------------------
SELECT IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'teacher_classes') > 0
  AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'sys_teacher_classes') > 0,
  'WARN: teacher_classes 与 sys_teacher_classes 同时存在，跳过改名',
  'OK: teacher_classes'
) AS align_note;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'teacher_classes') > 0
  AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'sys_teacher_classes') = 0,
  'RENAME TABLE teacher_classes TO sys_teacher_classes',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS sys_teacher_classes (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
  teacher_id BIGINT NOT NULL COMMENT '教师用户ID',
  class_id BIGINT NOT NULL COMMENT '班级ID',
  grade VARCHAR(20) NULL COMMENT '年级名称冗余，以 class_id 为准',
  class_name VARCHAR(50) NULL COMMENT '班级名称冗余，以 class_id 为准',
  subject VARCHAR(100) NULL COMMENT '任教科目',
  is_class_teacher TINYINT NOT NULL DEFAULT 0 COMMENT '是否班主任：0否 1是',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_teacher_id_class_id (teacher_id, class_id),
  KEY idx_teacher_id (teacher_id),
  KEY idx_class_id (class_id),
  CONSTRAINT fk_sys_teacher_classes_teacher FOREIGN KEY (teacher_id) REFERENCES sys_users (id),
  CONSTRAINT fk_sys_teacher_classes_class FOREIGN KEY (class_id) REFERENCES classes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师可访问的班级';

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_teacher_classes' AND column_name = 'class_id') = 0,
  'ALTER TABLE sys_teacher_classes ADD COLUMN class_id BIGINT NULL COMMENT ''班级ID'' AFTER teacher_id',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE sys_teacher_classes tc
JOIN classes c ON c.class_name = tc.class_name
JOIN grades g ON g.id = c.grade_id AND (tc.grade = g.grade_name OR tc.grade = g.grade_code)
SET tc.class_id = c.id
WHERE tc.class_id IS NULL AND tc.class_name IS NOT NULL;

SET @teacher_null_class := (SELECT COUNT(*) FROM sys_teacher_classes WHERE class_id IS NULL);
SET @sql := IF(
  @teacher_null_class = 0
  AND (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'sys_teacher_classes' AND index_name = 'uk_teacher_class') > 0,
  'ALTER TABLE sys_teacher_classes DROP INDEX uk_teacher_class',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  @teacher_null_class = 0
  AND (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'sys_teacher_classes' AND index_name = 'uk_teacher_id_class_id') = 0,
  'ALTER TABLE sys_teacher_classes MODIFY class_id BIGINT NOT NULL COMMENT ''班级ID'', ADD UNIQUE KEY uk_teacher_id_class_id (teacher_id, class_id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_teacher_classes' AND column_name = 'grade' AND is_nullable = 'NO') > 0,
  'ALTER TABLE sys_teacher_classes MODIFY grade VARCHAR(20) NULL COMMENT ''年级名称冗余，以 class_id 为准'', MODIFY class_name VARCHAR(50) NULL COMMENT ''班级名称冗余，以 class_id 为准''',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE sys_teacher_classes COMMENT = '教师可访问的班级';

SELECT IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'grade_admin_grades') > 0
  AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'sys_affairs_grades') > 0,
  'WARN: grade_admin_grades 与 sys_affairs_grades 同时存在，跳过改名',
  'OK: grade_admin_grades'
) AS align_note;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'grade_admin_grades') > 0
  AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'sys_affairs_grades') = 0,
  'RENAME TABLE grade_admin_grades TO sys_affairs_grades',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS sys_affairs_grades (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
  user_id BIGINT NOT NULL COMMENT '教务用户ID（角色 ACADEMIC）',
  grade_id BIGINT NOT NULL COMMENT '年级ID',
  grade VARCHAR(20) NULL COMMENT '年级名称冗余，以 grade_id 为准',
  department VARCHAR(100) NULL COMMENT '部门说明，可空',
  description VARCHAR(255) NULL COMMENT '说明',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_affairs_user_grade (user_id, grade_id),
  KEY idx_user_id (user_id),
  KEY idx_grade_id (grade_id),
  CONSTRAINT fk_sys_affairs_grades_user FOREIGN KEY (user_id) REFERENCES sys_users (id),
  CONSTRAINT fk_sys_affairs_grades_grade FOREIGN KEY (grade_id) REFERENCES grades (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教务可访问的年级';

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_affairs_grades' AND column_name = 'admin_id') > 0
  AND (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_affairs_grades' AND column_name = 'user_id') = 0,
  'ALTER TABLE sys_affairs_grades CHANGE COLUMN admin_id user_id BIGINT NOT NULL COMMENT ''教务用户ID（角色 ACADEMIC）''',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_affairs_grades' AND column_name = 'grade_id') = 0,
  'ALTER TABLE sys_affairs_grades ADD COLUMN grade_id BIGINT NULL COMMENT ''年级ID'' AFTER user_id',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE sys_affairs_grades a
JOIN grades g ON a.grade = g.grade_name OR a.grade = g.grade_code
SET a.grade_id = g.id
WHERE a.grade_id IS NULL AND a.grade IS NOT NULL;

SET @affairs_null_grade := (SELECT COUNT(*) FROM sys_affairs_grades WHERE grade_id IS NULL);
SET @sql := IF(
  @affairs_null_grade = 0,
  'ALTER TABLE sys_affairs_grades MODIFY grade_id BIGINT NOT NULL COMMENT ''年级ID''',
  'SELECT ''WARN: sys_affairs_grades.grade_id 仍有空值，未改为 NOT NULL'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  @affairs_null_grade = 0
  AND (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'sys_affairs_grades' AND index_name = 'uk_affairs_user_grade') = 0,
  'ALTER TABLE sys_affairs_grades ADD UNIQUE KEY uk_affairs_user_grade (user_id, grade_id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE sys_affairs_grades COMMENT = '教务可访问的年级';

-- -----------------------------------------------------------------------------
-- 公告
-- -----------------------------------------------------------------------------
SELECT IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'announcements') > 0
  AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'oa_announcements') > 0,
  'WARN: announcements 与 oa_announcements 同时存在，跳过改名',
  'OK: announcements'
) AS align_note;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'announcements') > 0
  AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'oa_announcements') = 0,
  'RENAME TABLE announcements TO oa_announcements',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS oa_announcements (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '公告ID',
  title VARCHAR(200) NOT NULL COMMENT '标题',
  content TEXT NOT NULL COMMENT '正文',
  category VARCHAR(50) NULL COMMENT '分类',
  publisher_id BIGINT NOT NULL COMMENT '发布人用户ID',
  publish_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  expire_time DATETIME NULL COMMENT '过期时间',
  is_top TINYINT NOT NULL DEFAULT 0 COMMENT '置顶：0否 1是',
  is_urgent TINYINT NOT NULL DEFAULT 0 COMMENT '紧急：0否 1是',
  target_audience VARCHAR(50) NULL COMMENT '遗留受众字符串。首期以 oa_announcement_scopes 为准',
  view_count INT NOT NULL DEFAULT 0 COMMENT '浏览次数',
  attachment_url VARCHAR(500) NULL COMMENT '附件 URL',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '0草稿DRAFT 1已发布PUBLISHED 2已撤回WITHDRAWN',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_publisher_id (publisher_id),
  KEY idx_publish_time (publish_time),
  KEY idx_status (status),
  KEY idx_is_top (is_top),
  CONSTRAINT fk_oa_announcements_publisher FOREIGN KEY (publisher_id) REFERENCES sys_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告';

ALTER TABLE oa_announcements
  COMMENT = '公告。受众以 oa_announcement_scopes 为准';

CREATE TABLE IF NOT EXISTS oa_announcement_scopes (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '受众ID',
  announcement_id BIGINT NOT NULL COMMENT '公告ID',
  scope_type TINYINT NOT NULL COMMENT '1 ALL 2 GRADE 3 CLASS',
  scope_id BIGINT NOT NULL DEFAULT 0 COMMENT 'ALL 固定 0；GRADE 为 grades.id；CLASS 为 classes.id',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_announcement_scope (announcement_id, scope_type, scope_id),
  KEY idx_scope (scope_type, scope_id),
  CONSTRAINT chk_oa_scope_type CHECK (scope_type BETWEEN 1 AND 3),
  CONSTRAINT fk_oa_scopes_announcement FOREIGN KEY (announcement_id) REFERENCES oa_announcements (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告受众';

CREATE TABLE IF NOT EXISTS oa_announcement_reads (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '已读ID',
  announcement_id BIGINT NOT NULL COMMENT '公告ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  read_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '已读时间',
  UNIQUE KEY uk_announcement_user (announcement_id, user_id),
  KEY idx_user_id (user_id),
  CONSTRAINT fk_oa_reads_announcement FOREIGN KEY (announcement_id) REFERENCES oa_announcements (id),
  CONSTRAINT fk_oa_reads_user FOREIGN KEY (user_id) REFERENCES sys_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告已读';

INSERT INTO oa_announcement_scopes (announcement_id, scope_type, scope_id)
SELECT a.id, 1, 0
FROM oa_announcements a
WHERE LOWER(a.target_audience) IN ('all', '全部')
  AND NOT EXISTS (
    SELECT 1 FROM oa_announcement_scopes s WHERE s.announcement_id = a.id
  );

-- -----------------------------------------------------------------------------
-- 审计
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_audit_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '审计ID',
  event_type VARCHAR(32) NOT NULL COMMENT 'LOGIN_FAIL/PASSWORD_RESET/STUDENT_IMPORT/STUDENT_EXPORT/STUDENT_STATUS_CHANGE/STUDENT_PROFILE_CHANGE，须与 event_code 成对',
  event_code TINYINT NOT NULL COMMENT '1 LOGIN_FAIL 2 PASSWORD_RESET 3 STUDENT_IMPORT 4 STUDENT_EXPORT 5 STUDENT_STATUS_CHANGE 6 STUDENT_PROFILE_CHANGE',
  actor_user_id BIGINT NULL COMMENT '操作者。登录失败时可能为空',
  target_user_id BIGINT NULL COMMENT '被操作用户',
  target_type VARCHAR(32) NULL COMMENT '对象类型，如 USER/STUDENT',
  target_id BIGINT NULL COMMENT '对象ID',
  result TINYINT NOT NULL COMMENT '0失败 1成功',
  summary VARCHAR(255) NULL COMMENT '一句话摘要',
  detail VARCHAR(1000) NULL COMMENT '补充说明。禁止写入身份证号、手机号明文',
  ip_address VARCHAR(64) NULL COMMENT '来源 IP',
  user_agent VARCHAR(255) NULL COMMENT 'User-Agent',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
  KEY idx_event_code_created (event_code, created_at),
  KEY idx_actor_user_id (actor_user_id),
  KEY idx_target (target_type, target_id),
  CONSTRAINT chk_sys_audit_event CHECK (event_code BETWEEN 1 AND 6),
  CONSTRAINT chk_sys_audit_result CHECK (result IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志。不设外键，避免用户删除后丢掉记录';

-- -----------------------------------------------------------------------------
-- 角色收敛为四档，并补齐按钮权限
-- -----------------------------------------------------------------------------
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'roles' AND column_name = 'role_level') = 0,
  'ALTER TABLE roles ADD COLUMN role_level TINYINT NOT NULL DEFAULT 0 COMMENT ''档位，数值越大越高：ADMIN 40 > ACADEMIC 30 > TEACHER 20 > STUDENT 10'' AFTER description',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO roles (role_code, role_name, description, role_level, status) VALUES
  ('ADMIN', '系统管理员', '全校数据与账号、角色配置', 40, 1),
  ('ACADEMIC', '教务', '授权年级内的学籍、课程、考勤与公告', 30, 1),
  ('TEACHER', '教师', '授权班级与所授课程', 20, 1),
  ('STUDENT', '学生', '本人学籍、课表、考勤与公告。家长使用同角色账号，无家长绑定表', 10, 1)
ON DUPLICATE KEY UPDATE
  role_name = VALUES(role_name),
  description = VALUES(description),
  role_level = VALUES(role_level),
  status = VALUES(status);

UPDATE roles
SET status = 0,
    role_level = 0,
    description = '已停用。首期四档无此角色，年级范围改由 ACADEMIC + sys_affairs_grades 承担。'
WHERE role_code = 'GRADE_ADMIN';

UPDATE roles
SET status = 0,
    role_level = 0,
    description = '已停用。访客/后勤为 phase:2，首期不启用。'
WHERE role_code = 'LOGISTICS';

INSERT INTO permissions (permission_code, permission_name, resource_type, resource_path, description, sort_order, status) VALUES
  ('org:write', '维护年级与班级', 'button', '/api/v1/grades', '年级、班级写操作', 10, 1),
  ('student:write', '维护学籍', 'button', '/api/v1/students', '学生档案写操作', 20, 1),
  ('student:import', '导入学籍', 'button', '/api/v1/students/batch-import', '批量导入', 30, 1),
  ('student:export', '导出学籍', 'button', '/api/v1/students/export', '导出须脱敏', 40, 1),
  ('course:write', '维护课程库', 'button', '/api/v1/courses', '课程库写操作', 50, 1),
  ('class_course:write', '维护排课', 'button', '/api/v1/class-courses', '班级授课与节次', 60, 1),
  ('attendance:record', '登记考勤', 'button', '/api/v1/attendances', '日报与异常', 70, 1),
  ('announcement:publish', '发布公告', 'button', '/api/v1/announcements', '按授权范围发布', 80, 1),
  ('announcement:top', '置顶公告', 'button', '/api/v1/announcements', '置顶', 90, 1),
  ('announcement:withdraw', '撤回公告', 'button', '/api/v1/announcements', '撤回', 100, 1),
  ('user:write', '维护账号', 'button', '/api/v1/users', '账号启停', 110, 1),
  ('role:write', '维护角色权限', 'button', '/api/v1/roles', '角色与权限码', 120, 1)
ON DUPLICATE KEY UPDATE
  permission_name = VALUES(permission_name),
  resource_type = VALUES(resource_type),
  resource_path = VALUES(resource_path),
  description = VALUES(description),
  sort_order = VALUES(sort_order),
  status = VALUES(status);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.permission_code IN (
  'org:write', 'student:write', 'student:import', 'student:export',
  'course:write', 'class_course:write', 'attendance:record',
  'announcement:publish', 'announcement:top', 'announcement:withdraw',
  'user:write', 'role:write'
)
WHERE r.role_code = 'ADMIN'
ON DUPLICATE KEY UPDATE role_id = role_id;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.permission_code IN (
  'org:write', 'student:write', 'student:import', 'student:export',
  'course:write', 'class_course:write', 'attendance:record',
  'announcement:publish', 'announcement:top', 'announcement:withdraw',
  'user:write'
)
WHERE r.role_code = 'ACADEMIC'
ON DUPLICATE KEY UPDATE role_id = role_id;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.permission_code IN (
  'student:export', 'attendance:record', 'announcement:publish'
)
WHERE r.role_code = 'TEACHER'
ON DUPLICATE KEY UPDATE role_id = role_id;

-- -----------------------------------------------------------------------------
-- 旧表只改注释，不扩展成绩 / 访客，也不把 scope_json 升格为主授权
-- -----------------------------------------------------------------------------
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'exam_scores') > 0,
  'ALTER TABLE exam_scores COMMENT ''考试成绩（phase:2，非首期必跑，本脚本不扩展列）''',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'exam_schedules') > 0,
  'ALTER TABLE exam_schedules COMMENT ''考试安排（phase:2，非首期必跑，本脚本不扩展列）''',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'visitor_registrations') > 0,
  'ALTER TABLE visitor_registrations COMMENT ''来访登记（phase:2，非首期必跑，本脚本不扩展列）''',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'data_query_permissions') > 0,
  'ALTER TABLE data_query_permissions COMMENT ''遗留查询范围。首期主授权是 sys_teacher_classes 与 sys_affairs_grades，不用 scope_value/scope_json''',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'password_reset_logs') > 0,
  'ALTER TABLE password_reset_logs COMMENT ''遗留密码重置日志。首期新事件写入 sys_audit_logs（event_code=2）''',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
