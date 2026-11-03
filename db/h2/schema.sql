-- =============================================================================
-- 校园 OA 首期结构（H2，本地）
-- 文件：db/h2/schema.sql
--
-- 权威来源是 db/migration，不是本文件。改表先改 migration，再按 db/README.md 的方言清单转译到这里。
-- 表名、字段、唯一键须与 db/migration/V1__phase1_schema.sql 一致。
-- 相对 MySQL 脚本的差异：
--   去掉 ENGINE / CHARSET / utf8mb4、ON UPDATE CURRENT_TIMESTAMP、SET NAMES、PREPARE。
--   枚举仍用 TINYINT，不用 BOOLEAN。自增仍写 AUTO_INCREMENT。
--   H2 的索引名在整个库里必须唯一，所以非唯一索引加上表名前缀。
--     唯一键名字（uk_*）本来就不重复，保持原名。
--   course_key 仍是 IFNULL(course_id, 0)，H2 省略 MySQL 的 STORED 关键字。
-- 连接串：jdbc:h2:mem:camp;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. 账号底座（只保留登录与角色挂接，不含密码重置/首次激活流程）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  username VARCHAR(50) NOT NULL COMMENT '登录名',
  password VARCHAR(255) NOT NULL COMMENT '密码哈希。算法由应用层决定，库只存哈希',
  real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
  id_card VARCHAR(18) NULL COMMENT '身份证号。敏感字段：库内可存，列表/导出脱敏由应用层按角色处理，禁止写入审计明文',
  phone VARCHAR(20) NULL COMMENT '手机号。敏感字段：库内可存，列表/导出脱敏由应用层按角色处理，禁止写入审计明文',
  email VARCHAR(100) NULL COMMENT '邮箱',
  user_type TINYINT NOT NULL DEFAULT 1 COMMENT '粗分类：1学生 2教职工 3管理员。授权以 roles 为准，不单独增加家长类型',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '账号状态：0禁用 1启用',
  avatar_url VARCHAR(255) NULL COMMENT '头像 URL',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_username (username),
  KEY sys_users_idx_user_type (user_type),
  KEY sys_users_idx_status (status)
) COMMENT='用户账号底座';

-- -----------------------------------------------------------------------------
-- 2. 角色与按钮权限（四档）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS roles (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
  role_code VARCHAR(50) NOT NULL COMMENT '角色代码',
  role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
  description VARCHAR(255) NULL COMMENT '说明',
  role_level TINYINT NOT NULL DEFAULT 0 COMMENT '档位，数值越大越高：ADMIN 40 > ACADEMIC 30 > TEACHER 20 > STUDENT 10',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '0禁用 1启用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_role_code (role_code),
  KEY roles_idx_status (status)
) COMMENT='角色。首期仅 ADMIN/ACADEMIC/TEACHER/STUDENT';

CREATE TABLE IF NOT EXISTS permissions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '权限ID',
  permission_code VARCHAR(100) NOT NULL COMMENT '权限码',
  permission_name VARCHAR(100) NOT NULL COMMENT '权限名称',
  resource_type VARCHAR(50) NOT NULL COMMENT '资源类型：menu/button/data',
  resource_path VARCHAR(200) NULL COMMENT '资源路径',
  parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父权限ID，0 为根',
  description VARCHAR(255) NULL COMMENT '说明',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '0禁用 1启用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_permission_code (permission_code),
  KEY permissions_idx_parent_id (parent_id),
  KEY permissions_idx_resource_type (resource_type)
) COMMENT='权限';

CREATE TABLE IF NOT EXISTS role_permissions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  permission_id BIGINT NOT NULL COMMENT '权限ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_role_permission (role_id, permission_id),
  KEY role_permissions_idx_role_id (role_id),
  KEY role_permissions_idx_permission_id (permission_id),
  CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles (id),
  CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id)
) COMMENT='角色-权限';

CREATE TABLE IF NOT EXISTS user_roles (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_user_role (user_id, role_id),
  KEY user_roles_idx_user_id (user_id),
  KEY user_roles_idx_role_id (role_id),
  CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES sys_users (id),
  CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
) COMMENT='用户-角色';

-- -----------------------------------------------------------------------------
-- 3. 年级 / 班级
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
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_grade_code (grade_code),
  KEY grades_idx_grade_type (grade_type),
  KEY grades_idx_grade_level (grade_level),
  KEY grades_idx_status (status)
) COMMENT='年级';

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
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_class_code (class_code),
  UNIQUE KEY uk_grade_class_number (grade_id, class_number),
  KEY classes_idx_grade_id (grade_id),
  KEY classes_idx_head_teacher_id (head_teacher_id),
  KEY classes_idx_status (status),
  CONSTRAINT fk_classes_grade FOREIGN KEY (grade_id) REFERENCES grades (id),
  CONSTRAINT fk_classes_head_teacher FOREIGN KEY (head_teacher_id) REFERENCES sys_users (id)
) COMMENT='班级';

-- -----------------------------------------------------------------------------
-- 4. 学生与应急联系人
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS students (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '学生ID',
  user_id BIGINT NULL COMMENT '关联用户ID。可空：允许先建学籍再开账号',
  student_no VARCHAR(20) NOT NULL COMMENT '学号',
  grade_id BIGINT NULL COMMENT '年级ID',
  grade VARCHAR(20) NULL COMMENT '年级名称冗余，以 grade_id 为准',
  class_id BIGINT NULL COMMENT '班级ID',
  class_name VARCHAR(50) NULL COMMENT '班级名称冗余，以 class_id 为准',
  major VARCHAR(100) NULL COMMENT '专业',
  department VARCHAR(100) NULL COMMENT '院系',
  enrollment_date DATE NULL COMMENT '入学日期',
  graduation_date DATE NULL COMMENT '毕业日期',
  dormitory VARCHAR(50) NULL COMMENT '宿舍',
  address VARCHAR(255) NULL COMMENT '家庭地址。敏感字段，展示范围由应用层控制',
  political_status VARCHAR(20) NULL COMMENT '政治面貌',
  nationality VARCHAR(50) NULL DEFAULT '中国' COMMENT '国籍',
  gender TINYINT NULL COMMENT '性别：0女 1男',
  birth_date DATE NULL COMMENT '出生日期',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '学籍：0休学SUSPENDED 1在读ENROLLED 2毕业GRADUATED 3退学WITHDRAWN',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '软删：0否 1是',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_student_no (student_no),
  UNIQUE KEY uk_student_user_id (user_id),
  KEY students_idx_grade_id (grade_id),
  KEY students_idx_class_id (class_id),
  KEY students_idx_status (status),
  KEY students_idx_is_deleted (is_deleted),
  CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES sys_users (id),
  CONSTRAINT fk_students_grade FOREIGN KEY (grade_id) REFERENCES grades (id),
  CONSTRAINT fk_students_class FOREIGN KEY (class_id) REFERENCES classes (id)
) COMMENT='学生学籍';

CREATE TABLE IF NOT EXISTS emergency_contacts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '联系人ID',
  student_id BIGINT NOT NULL COMMENT '学生ID',
  contact_name VARCHAR(50) NOT NULL COMMENT '联系人姓名',
  relationship VARCHAR(20) NOT NULL COMMENT '关系',
  phone VARCHAR(20) NOT NULL COMMENT '联系电话。敏感字段：库内可存，列表/导出脱敏由应用层处理',
  alternate_phone VARCHAR(20) NULL COMMENT '备用电话。敏感字段，脱敏规则同 phone',
  address VARCHAR(255) NULL COMMENT '联系地址',
  is_primary TINYINT NOT NULL DEFAULT 0 COMMENT '是否主要联系人：0否 1是',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY emergency_contacts_idx_student_id (student_id),
  KEY emergency_contacts_idx_is_primary (is_primary),
  CONSTRAINT fk_emergency_contacts_student FOREIGN KEY (student_id) REFERENCES students (id)
) COMMENT='学生应急联系人。家长登录不建绑定表，另发 STUDENT 账号';

-- -----------------------------------------------------------------------------
-- 5. 课程库与排课（节次在子表，禁止用 schedule_info 字符串当课表）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS courses (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '课程ID',
  course_code VARCHAR(20) NOT NULL COMMENT '课程代码',
  course_name VARCHAR(100) NOT NULL COMMENT '课程名称',
  credit DECIMAL(3,1) NOT NULL COMMENT '学分',
  course_type VARCHAR(20) NULL COMMENT '课程类型',
  department VARCHAR(100) NULL COMMENT '开课单位',
  teacher_id BIGINT NULL COMMENT '默认教师，可空。排课教师以 edu_class_courses.teacher_id 为准',
  semester VARCHAR(20) NULL COMMENT '遗留字段，不是排课来源',
  academic_year VARCHAR(20) NULL COMMENT '遗留字段，不是排课来源',
  description TEXT NULL COMMENT '简介',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_course_code (course_code),
  KEY courses_idx_teacher_id (teacher_id),
  KEY courses_idx_status (status),
  CONSTRAINT fk_courses_teacher FOREIGN KEY (teacher_id) REFERENCES sys_users (id)
) COMMENT='课程库。课表见 edu_class_courses + edu_class_course_sessions';

CREATE TABLE IF NOT EXISTS edu_class_courses (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '班级授课ID',
  class_id BIGINT NOT NULL COMMENT '班级ID',
  course_id BIGINT NOT NULL COMMENT '课程ID',
  teacher_id BIGINT NULL COMMENT '授课教师用户ID',
  academic_year VARCHAR(20) NOT NULL COMMENT '学年，如 2026-2027。不用 semester_id',
  semester TINYINT NOT NULL COMMENT '学期：1第一学期 2第二学期',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_class_course_term (class_id, course_id, academic_year, semester),
  KEY edu_class_courses_idx_class_id (class_id),
  KEY edu_class_courses_idx_course_id (course_id),
  KEY edu_class_courses_idx_teacher_id (teacher_id),
  KEY edu_class_courses_idx_term (academic_year, semester),
  CONSTRAINT chk_edu_class_course_semester CHECK (semester IN (1, 2)),
  CONSTRAINT fk_edu_class_courses_class FOREIGN KEY (class_id) REFERENCES classes (id),
  CONSTRAINT fk_edu_class_courses_course FOREIGN KEY (course_id) REFERENCES courses (id),
  CONSTRAINT fk_edu_class_courses_teacher FOREIGN KEY (teacher_id) REFERENCES sys_users (id)
) COMMENT='班级授课。一学期一门课一行，节次在子表';

CREATE TABLE IF NOT EXISTS edu_class_course_sessions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '节次ID',
  class_course_id BIGINT NOT NULL COMMENT '班级授课ID',
  weekday TINYINT NOT NULL COMMENT '星期：1周一 … 7周日',
  period_start TINYINT NOT NULL COMMENT '开始节次，从 1 起',
  period_end TINYINT NOT NULL COMMENT '结束节次，含本课',
  room_no VARCHAR(32) NULL COMMENT '教室',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_class_course_session (class_course_id, weekday, period_start, period_end),
  KEY edu_class_course_sessions_idx_class_course_id (class_course_id),
  KEY edu_class_course_sessions_idx_weekday_period (weekday, period_start, period_end),
  CONSTRAINT chk_edu_session_slot CHECK (
    weekday BETWEEN 1 AND 7
    AND period_start >= 1
    AND period_end >= period_start
  ),
  CONSTRAINT fk_edu_sessions_class_course FOREIGN KEY (class_course_id) REFERENCES edu_class_courses (id)
) COMMENT='班级授课节次';

-- -----------------------------------------------------------------------------
-- 6. 考勤：班级×日汇总，不挂课程；异常在明细，course_id 可空
-- -----------------------------------------------------------------------------
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
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_class_id_attendance_date (class_id, attendance_date),
  KEY edu_attendance_summaries_idx_attendance_date (attendance_date),
  KEY edu_attendance_summaries_idx_recorder_id (recorder_id),
  CONSTRAINT fk_edu_attendance_summaries_class FOREIGN KEY (class_id) REFERENCES classes (id),
  CONSTRAINT fk_edu_attendance_summaries_recorder FOREIGN KEY (recorder_id) REFERENCES sys_users (id)
) COMMENT='班级考勤日报。幂等键 class_id + attendance_date，不挂 course_id';

CREATE TABLE IF NOT EXISTS edu_attendance_records (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '异常ID',
  summary_id BIGINT NULL COMMENT '所属日报，可空以便补录',
  student_id BIGINT NOT NULL COMMENT '学生ID',
  attendance_date DATE NOT NULL COMMENT '考勤日期',
  course_id BIGINT NULL COMMENT '课程ID。空表示当日异常，非空表示某一节课',
  course_key BIGINT AS (IFNULL(course_id, 0)) COMMENT '唯一约束辅助：0 表示未挂课程。H2 不写 STORED，表达式与 MySQL 相同',
  exception_type TINYINT NOT NULL COMMENT '异常：1 LATE 2 EARLY_LEAVE 3 PERSONAL_LEAVE 4 SICK_LEAVE 5 ABSENT',
  confirm_status TINYINT NOT NULL DEFAULT 2 COMMENT '明细状态：1 PENDING 2 CONFIRMED 3 CLEARED。首期默认 CONFIRMED',
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
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_student_date_course (student_id, attendance_date, course_key),
  KEY edu_attendance_records_idx_summary_id (summary_id),
  KEY edu_attendance_records_idx_attendance_date (attendance_date),
  KEY edu_attendance_records_idx_exception_type (exception_type),
  KEY edu_attendance_records_idx_course_id (course_id),
  KEY edu_attendance_records_idx_recorder_id (recorder_id),
  CONSTRAINT chk_edu_attendance_exception CHECK (exception_type BETWEEN 1 AND 5),
  CONSTRAINT chk_edu_attendance_confirm CHECK (confirm_status BETWEEN 1 AND 3),
  CONSTRAINT fk_edu_attendance_records_summary FOREIGN KEY (summary_id) REFERENCES edu_attendance_summaries (id),
  CONSTRAINT fk_edu_attendance_records_student FOREIGN KEY (student_id) REFERENCES students (id),
  CONSTRAINT fk_edu_attendance_records_course FOREIGN KEY (course_id) REFERENCES courses (id),
  CONSTRAINT fk_edu_attendance_records_contact FOREIGN KEY (notified_contact_id) REFERENCES emergency_contacts (id),
  CONSTRAINT fk_edu_attendance_records_recorder FOREIGN KEY (recorder_id) REFERENCES sys_users (id)
) COMMENT='考勤异常明细。只记异常，不记正常出勤';

-- -----------------------------------------------------------------------------
-- 7. 数据范围：教师-班级、教务-年级。不用 scope_json 当主授权
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_teacher_classes (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
  teacher_id BIGINT NOT NULL COMMENT '教师用户ID',
  class_id BIGINT NOT NULL COMMENT '班级ID',
  grade VARCHAR(20) NULL COMMENT '年级名称冗余，以 class_id 为准',
  class_name VARCHAR(50) NULL COMMENT '班级名称冗余，以 class_id 为准',
  subject VARCHAR(100) NULL COMMENT '任教科目',
  is_class_teacher TINYINT NOT NULL DEFAULT 0 COMMENT '是否班主任：0否 1是',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_teacher_id_class_id (teacher_id, class_id),
  KEY sys_teacher_classes_idx_teacher_id (teacher_id),
  KEY sys_teacher_classes_idx_class_id (class_id),
  CONSTRAINT fk_sys_teacher_classes_teacher FOREIGN KEY (teacher_id) REFERENCES sys_users (id),
  CONSTRAINT fk_sys_teacher_classes_class FOREIGN KEY (class_id) REFERENCES classes (id)
) COMMENT='教师可访问的班级';

CREATE TABLE IF NOT EXISTS sys_affairs_grades (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
  user_id BIGINT NOT NULL COMMENT '教务用户ID（角色 ACADEMIC）',
  grade_id BIGINT NOT NULL COMMENT '年级ID',
  grade VARCHAR(20) NULL COMMENT '年级名称冗余，以 grade_id 为准',
  department VARCHAR(100) NULL COMMENT '部门说明，可空',
  description VARCHAR(255) NULL COMMENT '说明',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_affairs_user_grade (user_id, grade_id),
  KEY sys_affairs_grades_idx_user_id (user_id),
  KEY sys_affairs_grades_idx_grade_id (grade_id),
  CONSTRAINT fk_sys_affairs_grades_user FOREIGN KEY (user_id) REFERENCES sys_users (id),
  CONSTRAINT fk_sys_affairs_grades_grade FOREIGN KEY (grade_id) REFERENCES grades (id)
) COMMENT='教务可访问的年级';

-- -----------------------------------------------------------------------------
-- 8. 公告、受众、已读
-- -----------------------------------------------------------------------------
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
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY oa_announcements_idx_publisher_id (publisher_id),
  KEY oa_announcements_idx_publish_time (publish_time),
  KEY oa_announcements_idx_status (status),
  KEY oa_announcements_idx_is_top (is_top),
  CONSTRAINT fk_oa_announcements_publisher FOREIGN KEY (publisher_id) REFERENCES sys_users (id)
) COMMENT='公告';

CREATE TABLE IF NOT EXISTS oa_announcement_scopes (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '受众ID',
  announcement_id BIGINT NOT NULL COMMENT '公告ID',
  scope_type TINYINT NOT NULL COMMENT '1 ALL 2 GRADE 3 CLASS',
  scope_id BIGINT NOT NULL DEFAULT 0 COMMENT 'ALL 固定 0；GRADE 为 grades.id；CLASS 为 classes.id',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_announcement_scope (announcement_id, scope_type, scope_id),
  KEY oa_announcement_scopes_idx_scope (scope_type, scope_id),
  CONSTRAINT chk_oa_scope_type CHECK (scope_type BETWEEN 1 AND 3),
  CONSTRAINT fk_oa_scopes_announcement FOREIGN KEY (announcement_id) REFERENCES oa_announcements (id)
) COMMENT='公告受众';

CREATE TABLE IF NOT EXISTS oa_announcement_reads (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '已读ID',
  announcement_id BIGINT NOT NULL COMMENT '公告ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  read_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '已读时间',
  UNIQUE KEY uk_announcement_user (announcement_id, user_id),
  KEY oa_announcement_reads_idx_user_id (user_id),
  CONSTRAINT fk_oa_reads_announcement FOREIGN KEY (announcement_id) REFERENCES oa_announcements (id),
  CONSTRAINT fk_oa_reads_user FOREIGN KEY (user_id) REFERENCES sys_users (id)
) COMMENT='公告已读';

-- -----------------------------------------------------------------------------
-- 9. 审计
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
  KEY sys_audit_logs_idx_event_code_created (event_code, created_at),
  KEY sys_audit_logs_idx_actor_user_id (actor_user_id),
  KEY sys_audit_logs_idx_target (target_type, target_id),
  CONSTRAINT chk_sys_audit_event CHECK (event_code BETWEEN 1 AND 6),
  CONSTRAINT chk_sys_audit_result CHECK (result IN (0, 1))
) COMMENT='审计日志。不设外键，避免用户删除后丢掉记录';

-- -----------------------------------------------------------------------------
-- 字典：四档角色与首期按钮权限
-- -----------------------------------------------------------------------------
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
