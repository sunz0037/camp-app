-- =============================================================================
-- 二期增量：考试安排、成绩、来访登记、用户主题
-- 文件：db/migration/V3__phase2_exam_visitor_settings.sql
--
-- 不修改首期表。空库在 V1 之后执行；旧库在 V2 之后执行。可重复执行。
-- H2 对应 db/h2/schema.sql 末尾同名表（索引名加了表前缀，无 ENGINE / ON UPDATE）。
-- =============================================================================

CREATE TABLE IF NOT EXISTS edu_exams (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '考试ID',
  exam_code VARCHAR(40) NOT NULL COMMENT '考试编码。省略时由应用生成',
  exam_name VARCHAR(100) NOT NULL COMMENT '考试名称',
  grade_id BIGINT NULL COMMENT '年级ID，空表示不限年级',
  class_id BIGINT NULL COMMENT '班级ID，空表示不限班级',
  course_id BIGINT NULL COMMENT '科目，对应 courses.id',
  subject_name VARCHAR(100) NULL COMMENT '科目名称。有 course_id 时与课程名对齐',
  exam_type VARCHAR(20) NOT NULL COMMENT 'MIDTERM期中 FINAL期末 MONTHLY月考 QUIZ测验',
  start_time DATETIME NOT NULL COMMENT '开始时间',
  end_time DATETIME NOT NULL COMMENT '结束时间',
  is_public TINYINT NOT NULL DEFAULT 0 COMMENT '是否公开：0否 1是。学生只能看公开安排',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '0 CANCELED 1 SCHEDULED 2 FINISHED',
  room_no VARCHAR(32) NULL COMMENT '考场',
  remark VARCHAR(500) NULL COMMENT '备注',
  created_by BIGINT NULL COMMENT '创建人用户ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_edu_exam_code (exam_code),
  KEY idx_grade_id (grade_id),
  KEY idx_class_id (class_id),
  KEY idx_course_id (course_id),
  KEY idx_exam_type (exam_type),
  KEY idx_status (status),
  KEY idx_start_time (start_time),
  CONSTRAINT chk_edu_exam_public CHECK (is_public IN (0, 1)),
  CONSTRAINT chk_edu_exam_status CHECK (status IN (0, 1, 2)),
  CONSTRAINT fk_edu_exams_grade FOREIGN KEY (grade_id) REFERENCES grades (id),
  CONSTRAINT fk_edu_exams_class FOREIGN KEY (class_id) REFERENCES classes (id),
  CONSTRAINT fk_edu_exams_course FOREIGN KEY (course_id) REFERENCES courses (id),
  CONSTRAINT fk_edu_exams_creator FOREIGN KEY (created_by) REFERENCES sys_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试安排';

CREATE TABLE IF NOT EXISTS edu_exam_scores (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '成绩ID',
  exam_id BIGINT NOT NULL COMMENT '考试ID',
  student_id BIGINT NOT NULL COMMENT '学生ID',
  score DECIMAL(6,2) NULL COMMENT '分数',
  grade_level VARCHAR(10) NULL COMMENT '等级，可空。未传时按分数生成 A/B/C/D/E',
  recorded_by BIGINT NULL COMMENT '录入人用户ID',
  recorded_at DATETIME NULL COMMENT '录入时间',
  remark VARCHAR(255) NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_edu_exam_score_pair (exam_id, student_id),
  KEY idx_student_id (student_id),
  KEY idx_recorded_by (recorded_by),
  CONSTRAINT chk_edu_exam_score_range CHECK (score IS NULL OR (score >= 0 AND score <= 150)),
  CONSTRAINT fk_edu_exam_scores_exam FOREIGN KEY (exam_id) REFERENCES edu_exams (id),
  CONSTRAINT fk_edu_exam_scores_student FOREIGN KEY (student_id) REFERENCES students (id),
  CONSTRAINT fk_edu_exam_scores_recorder FOREIGN KEY (recorded_by) REFERENCES sys_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试成绩';

CREATE TABLE IF NOT EXISTS oa_visitors (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '来访ID',
  visit_no VARCHAR(40) NOT NULL COMMENT '来访单号。省略时由应用生成',
  visitor_name VARCHAR(50) NOT NULL COMMENT '访客姓名',
  id_card VARCHAR(32) NULL COMMENT '证件号。列表脱敏，禁止写入审计明文',
  phone VARCHAR(20) NOT NULL COMMENT '电话。列表脱敏，禁止写入审计明文',
  purpose VARCHAR(200) NOT NULL COMMENT '事由',
  host_user_id BIGINT NULL COMMENT '被访人用户ID',
  host_name VARCHAR(50) NULL COMMENT '被访人姓名',
  appointment_time DATETIME NULL COMMENT '预约时间',
  arrival_time DATETIME NULL COMMENT '入校时间',
  leave_time DATETIME NULL COMMENT '离校时间',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0 PENDING 1 APPROVED 2 REJECTED 3 CHECKED_IN 4 CHECKED_OUT',
  remark VARCHAR(500) NULL COMMENT '备注',
  created_by BIGINT NULL COMMENT '登记人用户ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_oa_visit_no (visit_no),
  KEY idx_status (status),
  KEY idx_host_user_id (host_user_id),
  KEY idx_created_by (created_by),
  KEY idx_appointment_time (appointment_time),
  CONSTRAINT chk_oa_visitor_status CHECK (status IN (0, 1, 2, 3, 4)),
  CONSTRAINT fk_oa_visitors_host FOREIGN KEY (host_user_id) REFERENCES sys_users (id),
  CONSTRAINT fk_oa_visitors_creator FOREIGN KEY (created_by) REFERENCES sys_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='来访登记';

CREATE TABLE IF NOT EXISTS sys_user_settings (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '设置ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  theme_key VARCHAR(32) NOT NULL DEFAULT 'default' COMMENT 'default / dark / campus-green',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_sys_user_settings_user (user_id),
  CONSTRAINT chk_sys_user_theme CHECK (theme_key IN ('default', 'dark', 'campus-green')),
  CONSTRAINT fk_sys_user_settings_user FOREIGN KEY (user_id) REFERENCES sys_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户设置。换肤偏好，default 对应 v1.3.4';
