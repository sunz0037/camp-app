-- =============================================================================
-- 校园 OA 二期增量
-- 文件：db/migration/V3__phase2_schema.sql
--
-- 前置：空库已执行 V1__phase1_schema.sql，或旧库已执行 V2__phase1_legacy_align.sql。
-- 不要在没有 sys_users / roles / students / courses / classes 的库上跑本文件。
-- 本脚本可重复执行：已存在的列和表会跳过，不改首期列的含义，不删除业务行。
--
-- 不改、不删旧脚本里的 exam_scores、exam_schedules、visitor_registrations。
-- 二期考试与访客是下面的 edu_* / oa_* 新表，不从旧表迁数据。
--
-- 密码历史保留近 N 次由应用层执行。sys_password_policy.history_size 默认 5，即 N=5。
-- 本表只追加哈希，不自动删除超出 N 的行。
--
-- H2 不跑本文件。本地结构在 db/h2/schema.sql，已按同一结果转译。
-- =============================================================================

SET NAMES utf8mb4;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name IN ('sys_users', 'roles', 'students', 'courses', 'classes')) < 5,
  'DROP TABLE `__phase2_v3_refuses_to_run_before_phase1_run_V1_or_V2_first__`',
  'SELECT ''V3 phase2 schema'' AS phase2_step'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 1. sys_users 密码策略增量列（可空时间 + 带默认值的计数/标志，不改原列）
-- -----------------------------------------------------------------------------
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'sys_users' AND column_name = 'password_changed_at') = 0,
  'ALTER TABLE sys_users ADD COLUMN password_changed_at DATETIME NULL COMMENT ''最近一次成功修改密码的时间，空表示尚未记录'' AFTER status',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'sys_users' AND column_name = 'password_expires_at') = 0,
  'ALTER TABLE sys_users ADD COLUMN password_expires_at DATETIME NULL COMMENT ''密码到期时间，空表示未单独写到期点'' AFTER password_changed_at',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'sys_users' AND column_name = 'failed_login_count') = 0,
  'ALTER TABLE sys_users ADD COLUMN failed_login_count INT NOT NULL DEFAULT 0 COMMENT ''连续登录失败次数，成功登录后由应用清零'' AFTER password_expires_at',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'sys_users' AND column_name = 'locked_until') = 0,
  'ALTER TABLE sys_users ADD COLUMN locked_until DATETIME NULL COMMENT ''锁定截止时间，空表示未锁定'' AFTER failed_login_count',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'sys_users' AND column_name = 'must_change_password') = 0,
  'ALTER TABLE sys_users ADD COLUMN must_change_password TINYINT NOT NULL DEFAULT 0 COMMENT ''下次登录必须改密：0否 1是'' AFTER locked_until',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 2. 密码策略（全库一行）与密码历史
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_password_policy (
  id TINYINT NOT NULL PRIMARY KEY COMMENT '固定为 1，全库一行',
  min_length TINYINT NOT NULL DEFAULT 8 COMMENT '最短长度',
  require_upper TINYINT NOT NULL DEFAULT 1 COMMENT '必须含大写：0否 1是',
  require_lower TINYINT NOT NULL DEFAULT 1 COMMENT '必须含小写：0否 1是',
  require_digit TINYINT NOT NULL DEFAULT 1 COMMENT '必须含数字：0否 1是',
  require_special TINYINT NOT NULL DEFAULT 1 COMMENT '必须含特殊字符：0否 1是',
  history_size TINYINT NOT NULL DEFAULT 5 COMMENT '禁止复用的最近次数。应用层对 sys_password_history 保留 N=5',
  max_failed TINYINT NOT NULL DEFAULT 5 COMMENT '连续失败上限，达到后锁定',
  lock_minutes INT NOT NULL DEFAULT 30 COMMENT '锁定分钟数',
  expire_days INT NOT NULL DEFAULT 90 COMMENT '密码有效天数。0 表示由应用视为不过期',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  CONSTRAINT chk_sys_password_policy_singleton CHECK (id = 1),
  CONSTRAINT chk_sys_password_policy_flags CHECK (
    require_upper IN (0, 1)
    AND require_lower IN (0, 1)
    AND require_digit IN (0, 1)
    AND require_special IN (0, 1)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='密码策略。只保留 id=1 一行，不按用户拆分';

-- 只在缺少默认行时插入。重复执行不覆盖管理员已改过的策略。
INSERT INTO sys_password_policy (
  id, min_length, require_upper, require_lower, require_digit, require_special,
  history_size, max_failed, lock_minutes, expire_days
)
SELECT 1, 8, 1, 1, 1, 1, 5, 5, 30, 90
WHERE NOT EXISTS (SELECT 1 FROM sys_password_policy WHERE id = 1);

CREATE TABLE IF NOT EXISTS sys_password_history (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '历史ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  password_hash VARCHAR(255) NOT NULL COMMENT '当时的密码哈希。禁止明文。应用层只保留该用户最近 history_size 次，默认 N=5',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '写入时间',
  KEY idx_user_created (user_id, created_at),
  CONSTRAINT fk_sys_password_history_user FOREIGN KEY (user_id) REFERENCES sys_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='密码历史。库不自动裁剪，保留近 N=5 次由应用层删除更早的行';

-- -----------------------------------------------------------------------------
-- 3. 用户偏好（只存主题 key，不存色板 hex）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user_preferences (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '偏好ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  theme_key VARCHAR(32) NOT NULL DEFAULT 'default' COMMENT '主题 key，如 default / dark / campus-blue。色值由前端 Token 解释，禁止存 hex',
  locale VARCHAR(16) NULL COMMENT '语言，如 zh-CN',
  extras TEXT NULL COMMENT '扩展偏好，JSON 文本。不存色板 hex',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_pref_user (user_id),
  CONSTRAINT fk_sys_user_preferences_user FOREIGN KEY (user_id) REFERENCES sys_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户偏好。一行一个用户';

-- -----------------------------------------------------------------------------
-- 4. 考试与成绩
-- 附件中的 term 拆成 academic_year + semester，与 edu_class_courses 一致。
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS edu_exams (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '考试ID',
  course_id BIGINT NOT NULL COMMENT '课程ID',
  class_id BIGINT NOT NULL COMMENT '班级ID',
  title VARCHAR(100) NOT NULL COMMENT '考试名称',
  exam_date DATE NOT NULL COMMENT '考试日期',
  academic_year VARCHAR(20) NOT NULL COMMENT '学年，如 2026-2027。对应附件里的 term，不另建学期字符串',
  semester TINYINT NOT NULL COMMENT '学期：1第一学期 2第二学期',
  created_by BIGINT NOT NULL COMMENT '创建人用户ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_edu_exam_natural (class_id, course_id, exam_date, title),
  KEY idx_course_id (course_id),
  KEY idx_class_id (class_id),
  KEY idx_exam_date (exam_date),
  CONSTRAINT chk_edu_exam_semester CHECK (semester IN (1, 2)),
  CONSTRAINT fk_edu_exams_course FOREIGN KEY (course_id) REFERENCES courses (id),
  CONSTRAINT fk_edu_exams_class FOREIGN KEY (class_id) REFERENCES classes (id),
  CONSTRAINT fk_edu_exams_creator FOREIGN KEY (created_by) REFERENCES sys_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级考试。成绩在 edu_exam_scores';

CREATE TABLE IF NOT EXISTS edu_exam_scores (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '成绩ID',
  exam_id BIGINT NOT NULL COMMENT '考试ID',
  student_id BIGINT NOT NULL COMMENT '学生ID',
  score DECIMAL(5,2) NOT NULL COMMENT '分数',
  rank_in_class INT NULL COMMENT '班内名次，可空',
  remark VARCHAR(255) NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_exam_student (exam_id, student_id),
  KEY idx_exam_id (exam_id),
  KEY idx_student_id (student_id),
  CONSTRAINT fk_edu_exam_scores_exam FOREIGN KEY (exam_id) REFERENCES edu_exams (id),
  CONSTRAINT fk_edu_exam_scores_student FOREIGN KEY (student_id) REFERENCES students (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试成绩。同一场考试每个学生一行';

-- -----------------------------------------------------------------------------
-- 5. 访客与流水
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS oa_visitors (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '访客ID',
  name VARCHAR(50) NOT NULL COMMENT '访客姓名',
  phone VARCHAR(20) NOT NULL COMMENT '手机号。敏感字段：库内可存，列表/导出脱敏由应用层处理，禁止写入审计明文',
  id_no VARCHAR(18) NULL COMMENT '证件号，可空。敏感字段，规则同 phone',
  org VARCHAR(100) NULL COMMENT '单位',
  purpose VARCHAR(200) NOT NULL COMMENT '来访事由',
  host_user_id BIGINT NOT NULL COMMENT '被访人用户ID',
  host_dept VARCHAR(100) NULL COMMENT '被访部门',
  expected_arrive DATETIME NOT NULL COMMENT '预计到达',
  expected_leave DATETIME NULL COMMENT '预计离开',
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/CHECKED_IN/CHECKED_OUT/CANCELLED',
  created_by BIGINT NOT NULL COMMENT '登记人用户ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_status (status),
  KEY idx_host_user_id (host_user_id),
  KEY idx_expected_arrive (expected_arrive),
  KEY idx_created_by (created_by),
  CONSTRAINT chk_oa_visitor_status CHECK (status IN (
    'PENDING', 'APPROVED', 'REJECTED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED'
  )),
  CONSTRAINT fk_oa_visitors_host FOREIGN KEY (host_user_id) REFERENCES sys_users (id),
  CONSTRAINT fk_oa_visitors_creator FOREIGN KEY (created_by) REFERENCES sys_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='访客登记';

CREATE TABLE IF NOT EXISTS oa_visitor_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '流水ID',
  visitor_id BIGINT NOT NULL COMMENT '访客ID',
  action VARCHAR(32) NOT NULL COMMENT '动作，如 SUBMIT/APPROVE/REJECT/CHECK_IN/CHECK_OUT/CANCEL。不做成枚举，避免加动作就要改表',
  operator_id BIGINT NOT NULL COMMENT '操作人用户ID',
  note VARCHAR(255) NULL COMMENT '说明',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
  KEY idx_visitor_id (visitor_id),
  KEY idx_operator_id (operator_id),
  CONSTRAINT fk_oa_visitor_logs_visitor FOREIGN KEY (visitor_id) REFERENCES oa_visitors (id),
  CONSTRAINT fk_oa_visitor_logs_operator FOREIGN KEY (operator_id) REFERENCES sys_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='访客状态流水。只追加，不更新';

-- -----------------------------------------------------------------------------
-- 6. 网盘文件与分享（只存元数据，不含对象存储实现）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS oa_files (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文件ID',
  owner_id BIGINT NOT NULL COMMENT '拥有者用户ID',
  parent_id BIGINT NULL COMMENT '父目录，根目录为空',
  name VARCHAR(255) NOT NULL COMMENT '显示名',
  storage_key VARCHAR(255) NOT NULL COMMENT '对象键。目录也要唯一键，不是对外 URL',
  mime VARCHAR(127) NULL COMMENT 'MIME，目录为空',
  size_bytes BIGINT NOT NULL DEFAULT 0 COMMENT '字节数，目录为 0',
  is_dir TINYINT NOT NULL DEFAULT 0 COMMENT '是否目录：0否 1是',
  deleted_at DATETIME NULL COMMENT '软删时间，空表示未删',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_storage_key (storage_key),
  KEY idx_owner_parent (owner_id, parent_id),
  KEY idx_parent_id (parent_id),
  CONSTRAINT chk_oa_file_is_dir CHECK (is_dir IN (0, 1)),
  CONSTRAINT chk_oa_file_size CHECK (size_bytes >= 0),
  CONSTRAINT fk_oa_files_owner FOREIGN KEY (owner_id) REFERENCES sys_users (id),
  CONSTRAINT fk_oa_files_parent FOREIGN KEY (parent_id) REFERENCES oa_files (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网盘文件元数据。路径由父目录链加 name 得到，不另存一份 path';

CREATE TABLE IF NOT EXISTS oa_file_shares (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分享ID',
  file_id BIGINT NOT NULL COMMENT '文件或目录ID',
  share_to_user_id BIGINT NULL COMMENT '分享给用户。与 share_to_role_id 二选一',
  share_to_role_id BIGINT NULL COMMENT '分享给角色。与 share_to_user_id 二选一',
  permission VARCHAR(8) NOT NULL COMMENT 'READ 或 WRITE',
  expires_at DATETIME NULL COMMENT '过期时间，空表示不过期',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_file_share_user (file_id, share_to_user_id),
  UNIQUE KEY uk_file_share_role (file_id, share_to_role_id),
  KEY idx_file_id (file_id),
  KEY idx_share_to_user_id (share_to_user_id),
  KEY idx_share_to_role_id (share_to_role_id),
  CONSTRAINT chk_oa_file_share_permission CHECK (permission IN ('READ', 'WRITE')),
  CONSTRAINT chk_oa_file_share_target CHECK (
    (share_to_user_id IS NOT NULL AND share_to_role_id IS NULL)
    OR (share_to_user_id IS NULL AND share_to_role_id IS NOT NULL)
  ),
  CONSTRAINT fk_oa_file_shares_file FOREIGN KEY (file_id) REFERENCES oa_files (id),
  CONSTRAINT fk_oa_file_shares_user FOREIGN KEY (share_to_user_id) REFERENCES sys_users (id),
  CONSTRAINT fk_oa_file_shares_role FOREIGN KEY (share_to_role_id) REFERENCES roles (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网盘分享。用户与角色只能填一边';
