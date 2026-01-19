-- ============================================
-- 校园OA系统数据库表结构设计
-- ============================================

-- 1. 用户表（存储所有用户信息：学生、教职工、管理员等）
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
    real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
    id_card VARCHAR(18) COMMENT '身份证号',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    user_type TINYINT NOT NULL DEFAULT 1 COMMENT '用户类型：1-学生，2-教职工，3-管理员',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    avatar_url VARCHAR(255) COMMENT '头像URL',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_user_type (user_type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 学生信息表
CREATE TABLE IF NOT EXISTS students (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '学生ID',
    user_id BIGINT NOT NULL UNIQUE COMMENT '关联用户ID',
    student_no VARCHAR(20) NOT NULL UNIQUE COMMENT '学号',
    grade VARCHAR(20) COMMENT '年级',
    class_name VARCHAR(50) COMMENT '班级',
    major VARCHAR(100) COMMENT '专业',
    department VARCHAR(100) COMMENT '院系',
    enrollment_date DATE COMMENT '入学日期',
    graduation_date DATE COMMENT '毕业日期',
    dormitory VARCHAR(50) COMMENT '宿舍号',
    address VARCHAR(255) COMMENT '家庭地址',
    political_status VARCHAR(20) COMMENT '政治面貌',
    nationality VARCHAR(50) DEFAULT '中国' COMMENT '国籍',
    gender TINYINT COMMENT '性别：0-女，1-男',
    birth_date DATE COMMENT '出生日期',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '学籍状态：0-休学，1-在读，2-毕业，3-退学',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_student_no (student_no),
    INDEX idx_grade (grade),
    INDEX idx_class_name (class_name),
    INDEX idx_major (major),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生信息表';

-- 3. 学生应急联系人表
CREATE TABLE IF NOT EXISTS emergency_contacts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '联系人ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    contact_name VARCHAR(50) NOT NULL COMMENT '联系人姓名',
    relationship VARCHAR(20) NOT NULL COMMENT '关系：父亲、母亲、监护人、其他',
    phone VARCHAR(20) NOT NULL COMMENT '联系电话',
    alternate_phone VARCHAR(20) COMMENT '备用电话',
    address VARCHAR(255) COMMENT '联系地址',
    is_primary TINYINT NOT NULL DEFAULT 0 COMMENT '是否主要联系人：0-否，1-是',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_student_id (student_id),
    INDEX idx_is_primary (is_primary)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生应急联系人表';

-- 4. 课程表
CREATE TABLE IF NOT EXISTS courses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '课程ID',
    course_code VARCHAR(20) NOT NULL UNIQUE COMMENT '课程代码',
    course_name VARCHAR(100) NOT NULL COMMENT '课程名称',
    credit DECIMAL(3,1) NOT NULL COMMENT '学分',
    course_type VARCHAR(20) COMMENT '课程类型：必修、选修、公共课等',
    department VARCHAR(100) COMMENT '开课院系',
    teacher_id BIGINT COMMENT '授课教师ID',
    semester VARCHAR(20) COMMENT '学期',
    academic_year VARCHAR(20) COMMENT '学年',
    description TEXT COMMENT '课程描述',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-停用，1-启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_course_code (course_code),
    INDEX idx_semester (semester),
    INDEX idx_academic_year (academic_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

-- 5. 考试成绩表
CREATE TABLE IF NOT EXISTS exam_scores (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '成绩ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    exam_type VARCHAR(20) NOT NULL COMMENT '考试类型：期中、期末、平时、实验等',
    score DECIMAL(5,2) COMMENT '成绩（可为空，表示未出成绩）',
    credit DECIMAL(3,1) COMMENT '获得学分',
    grade_point DECIMAL(3,2) COMMENT '绩点',
    exam_date DATE COMMENT '考试日期',
    semester VARCHAR(20) COMMENT '学期',
    academic_year VARCHAR(20) COMMENT '学年',
    remark VARCHAR(255) COMMENT '备注',
    created_by BIGINT COMMENT '录入人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_student_id (student_id),
    INDEX idx_course_id (course_id),
    INDEX idx_semester (semester),
    INDEX idx_academic_year (academic_year),
    UNIQUE KEY uk_student_course_exam (student_id, course_id, exam_type, semester, academic_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试成绩表';

-- 6. 校园公告表
CREATE TABLE IF NOT EXISTS announcements (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '公告ID',
    title VARCHAR(200) NOT NULL COMMENT '公告标题',
    content TEXT NOT NULL COMMENT '公告内容',
    category VARCHAR(50) COMMENT '公告分类：通知、新闻、活动、紧急等',
    publisher_id BIGINT NOT NULL COMMENT '发布人ID',
    publish_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    expire_time DATETIME COMMENT '过期时间',
    is_top TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶：0-否，1-是',
    is_urgent TINYINT NOT NULL DEFAULT 0 COMMENT '是否紧急：0-否，1-是',
    target_audience VARCHAR(50) COMMENT '目标受众：all-全部，student-学生，teacher-教职工',
    view_count INT NOT NULL DEFAULT 0 COMMENT '浏览次数',
    attachment_url VARCHAR(500) COMMENT '附件URL',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-草稿，1-已发布，2-已撤回',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_publisher_id (publisher_id),
    INDEX idx_category (category),
    INDEX idx_publish_time (publish_time),
    INDEX idx_status (status),
    INDEX idx_is_top (is_top),
    INDEX idx_is_urgent (is_urgent)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='校园公告表';

-- 7. 用户来访登记表
CREATE TABLE IF NOT EXISTS visitor_registrations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '登记ID',
    visitor_name VARCHAR(50) NOT NULL COMMENT '来访人姓名',
    visitor_phone VARCHAR(20) NOT NULL COMMENT '来访人电话',
    visitor_id_card VARCHAR(18) COMMENT '来访人身份证号',
    visit_purpose VARCHAR(200) NOT NULL COMMENT '来访目的',
    visit_target_user_id BIGINT COMMENT '访问目标用户ID',
    visit_target_name VARCHAR(50) COMMENT '访问目标姓名（冗余字段，便于查询）',
    visit_date DATE NOT NULL COMMENT '来访日期',
    visit_time_start DATETIME COMMENT '来访开始时间',
    visit_time_end DATETIME COMMENT '来访结束时间',
    entry_time DATETIME COMMENT '实际进入时间',
    exit_time DATETIME COMMENT '实际离开时间',
    vehicle_info VARCHAR(100) COMMENT '车辆信息',
    accompany_count INT DEFAULT 0 COMMENT '随行人数',
    receptionist_id BIGINT COMMENT '接待人ID',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待审核，1-已批准，2-已进入，3-已离开，4-已拒绝',
    remark VARCHAR(500) COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_visitor_name (visitor_name),
    INDEX idx_visitor_phone (visitor_phone),
    INDEX idx_visit_date (visit_date),
    INDEX idx_visit_target_user_id (visit_target_user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户来访登记表';

-- 8. 角色表
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
    role_code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色代码',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    description VARCHAR(255) COMMENT '角色描述',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_role_code (role_code),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 9. 权限表
CREATE TABLE IF NOT EXISTS permissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '权限ID',
    permission_code VARCHAR(100) NOT NULL UNIQUE COMMENT '权限代码',
    permission_name VARCHAR(100) NOT NULL COMMENT '权限名称',
    resource_type VARCHAR(50) NOT NULL COMMENT '资源类型：menu-菜单，button-按钮，data-数据',
    resource_path VARCHAR(200) COMMENT '资源路径',
    parent_id BIGINT DEFAULT 0 COMMENT '父权限ID',
    description VARCHAR(255) COMMENT '权限描述',
    sort_order INT DEFAULT 0 COMMENT '排序顺序',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_permission_code (permission_code),
    INDEX idx_parent_id (parent_id),
    INDEX idx_resource_type (resource_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- 10. 角色权限关联表
CREATE TABLE IF NOT EXISTS role_permissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    INDEX idx_role_id (role_id),
    INDEX idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- 11. 用户角色关联表
CREATE TABLE IF NOT EXISTS user_roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_role (user_id, role_id),
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 12. 教师班级关联表（用于权限控制：教师仅可查询本班级信息）
CREATE TABLE IF NOT EXISTS teacher_classes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
    teacher_id BIGINT NOT NULL COMMENT '教师用户ID',
    grade VARCHAR(20) NOT NULL COMMENT '年级',
    class_name VARCHAR(50) NOT NULL COMMENT '班级名称',
    subject VARCHAR(100) COMMENT '任教科目',
    is_class_teacher TINYINT NOT NULL DEFAULT 0 COMMENT '是否班主任：0-否，1-是',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_teacher_class (teacher_id, grade, class_name),
    INDEX idx_teacher_id (teacher_id),
    INDEX idx_grade_class (grade, class_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师班级关联表（权限最小化：教师仅可查询所管理班级的信息）';

-- 13. 年级管理员年级关联表（用于权限控制：年级管理员仅可查询该年级信息）
CREATE TABLE IF NOT EXISTS grade_admin_grades (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
    admin_id BIGINT NOT NULL COMMENT '年级管理员用户ID',
    grade VARCHAR(20) NOT NULL COMMENT '年级',
    department VARCHAR(100) COMMENT '所属院系',
    description VARCHAR(255) COMMENT '说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_admin_grade (admin_id, grade),
    INDEX idx_admin_id (admin_id),
    INDEX idx_grade (grade)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='年级管理员年级关联表（权限最小化：年级管理员仅可查询所管理年级的信息）';

-- 14. 数据查询权限管理表
CREATE TABLE IF NOT EXISTS data_query_permissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '权限ID',
    user_id BIGINT COMMENT '用户ID（为空表示角色权限）',
    role_id BIGINT COMMENT '角色ID（为空表示用户权限）',
    data_type VARCHAR(50) NOT NULL COMMENT '数据类型：student-学生信息，score-成绩，announcement-公告，visitor-来访登记等',
    query_scope VARCHAR(50) NOT NULL COMMENT '查询范围：all-全部，own-仅自己，department-本部门，grade-本年级，class-本班级，custom-自定义',
    scope_value VARCHAR(500) COMMENT '范围值（当query_scope为custom/grade/class时使用，JSON格式存储具体范围，如{"grade":"2024","class":"计算机1班"}）',
    field_permissions TEXT COMMENT '字段权限（JSON格式，控制可查询的字段，如["student_no","name","grade","class_name"]，空表示全部字段）',
    can_export TINYINT NOT NULL DEFAULT 0 COMMENT '是否可导出：0-否，1-是',
    can_print TINYINT NOT NULL DEFAULT 0 COMMENT '是否可打印：0-否，1-是',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id),
    INDEX idx_data_type (data_type),
    INDEX idx_query_scope (query_scope)
    -- 注意：CHECK 约束在 MySQL 5.7 及之前版本中会被忽略，在 MySQL 8.0+ 中才真正支持
    -- 如果需要兼容旧版本，请在应用层进行验证
    -- CHECK (user_id IS NOT NULL OR role_id IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据查询权限管理表（遵循权限最小化原则，防止信息泄露）';

-- 15. 班级每日考勤汇总表（按班级/日期汇总应到、实到、缺勤）
CREATE TABLE IF NOT EXISTS class_attendance_daily (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    attendance_date DATE NOT NULL COMMENT '考勤日期',
    grade VARCHAR(20) NOT NULL COMMENT '年级',
    class_name VARCHAR(50) NOT NULL COMMENT '班级',
    expected_count INT NOT NULL COMMENT '应到人数',
    present_count INT NOT NULL COMMENT '实到人数',
    absent_count INT NOT NULL DEFAULT 0 COMMENT '缺勤人数',
    late_count INT NOT NULL DEFAULT 0 COMMENT '迟到人数',
    leave_count INT NOT NULL DEFAULT 0 COMMENT '请假人数',
    recorder_id BIGINT COMMENT '记录人（教师/班主任）用户ID',
    remark VARCHAR(255) COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_class_date (attendance_date, grade, class_name),
    INDEX idx_attendance_date (attendance_date),
    INDEX idx_grade_class (grade, class_name),
    INDEX idx_recorder_id (recorder_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级每日考勤汇总表（用于统计当日实到/应到）';

-- 16. 学生每日考勤记录表（按学生/日期记录状态与缺勤原因）
CREATE TABLE IF NOT EXISTS student_attendance_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    attendance_date DATE NOT NULL COMMENT '考勤日期',
    grade VARCHAR(20) NOT NULL COMMENT '年级（冗余方便查询）',
    class_name VARCHAR(50) NOT NULL COMMENT '班级（冗余方便查询）',
    status TINYINT NOT NULL COMMENT '出勤状态（仅记录异常）：1-缺勤，2-迟到，3-请假，4-早退',
    reason_type VARCHAR(30) COMMENT '缺勤/异常原因类型：sick-生病，competition-比赛，personal-事假，other-其他',
    reason_detail VARCHAR(255) COMMENT '缺勤/异常具体原因描述',
    notified_contact_id BIGINT COMMENT '已通知的紧急联系人ID（可选）',
    notified_contact_name VARCHAR(50) COMMENT '已通知联系人姓名（冗余）',
    notified_method VARCHAR(30) COMMENT '通知方式：phone、sms、wechat等',
    notified_at DATETIME COMMENT '通知时间',
    attachment_url VARCHAR(255) COMMENT '证明材料（如病假条）',
    recorder_id BIGINT COMMENT '记录人（教师/班主任）用户ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_student_date (student_id, attendance_date),
    INDEX idx_attendance_date (attendance_date),
    INDEX idx_grade_class (grade, class_name),
    INDEX idx_status (status),
    INDEX idx_reason_type (reason_type)
    -- 注意：CHECK 约束在 MySQL 5.7 及之前版本中会被忽略，在 MySQL 8.0+ 中才真正支持
    -- 如果需要兼容旧版本，请在应用层进行验证
    -- CHECK (status IN (1,2,3,4))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生每日考勤记录表（按学生记录异常/缺勤原因，支持家长确认）';

-- ============================================
-- 初始化基础数据
-- ============================================

-- 插入默认角色
INSERT INTO roles (role_code, role_name, description) VALUES
('ADMIN', '系统管理员', '拥有系统所有权限'),
('TEACHER', '教师', '教师角色，仅可查询本班级相关信息（权限最小化原则）'),
('GRADE_ADMIN', '年级管理员', '年级管理员角色，仅可查询该年级相关信息（权限最小化原则）'),
('STUDENT', '学生', '学生角色，可查看个人信息和成绩'),
('LOGISTICS', '后勤人员', '后勤人员，可管理来访登记')
ON DUPLICATE KEY UPDATE role_name=role_name;

-- 插入基础权限（示例）
INSERT INTO permissions (permission_code, permission_name, resource_type, resource_path, description) VALUES
('student:view', '查看学生信息', 'data', '/api/students', '查看学生信息权限'),
('student:edit', '编辑学生信息', 'data', '/api/students', '编辑学生信息权限'),
('student:delete', '删除学生信息', 'data', '/api/students', '删除学生信息权限'),
('score:view', '查看成绩', 'data', '/api/scores', '查看成绩权限'),
('score:edit', '编辑成绩', 'data', '/api/scores', '编辑成绩权限'),
('announcement:view', '查看公告', 'data', '/api/announcements', '查看公告权限'),
('announcement:publish', '发布公告', 'data', '/api/announcements', '发布公告权限'),
('visitor:view', '查看来访登记', 'data', '/api/visitors', '查看来访登记权限'),
('visitor:manage', '管理来访登记', 'data', '/api/visitors', '管理来访登记权限'),
('permission:manage', '权限管理', 'data', '/api/permissions', '权限管理权限')
ON DUPLICATE KEY UPDATE permission_name=permission_name;

-- ============================================
-- 初始化数据查询权限配置（遵循权限最小化原则）
-- ============================================

-- 教师角色：仅可查询本班级相关信息
-- 注意：实际使用时，需要在用户表中记录教师所管理的班级信息，或通过user_roles关联表扩展字段
INSERT INTO data_query_permissions (role_id, data_type, query_scope, scope_value, field_permissions, can_export, can_print) 
SELECT r.id, 'student', 'class', NULL, NULL, 0, 0
FROM roles r WHERE r.role_code = 'TEACHER'
ON DUPLICATE KEY UPDATE query_scope=query_scope;

INSERT INTO data_query_permissions (role_id, data_type, query_scope, scope_value, field_permissions, can_export, can_print) 
SELECT r.id, 'score', 'class', NULL, NULL, 0, 0
FROM roles r WHERE r.role_code = 'TEACHER'
ON DUPLICATE KEY UPDATE query_scope=query_scope;

INSERT INTO data_query_permissions (role_id, data_type, query_scope, scope_value, field_permissions, can_export, can_print) 
SELECT r.id, 'emergency_contact', 'class', NULL, NULL, 0, 0
FROM roles r WHERE r.role_code = 'TEACHER'
ON DUPLICATE KEY UPDATE query_scope=query_scope;

-- 年级管理员角色：仅可查询该年级相关信息
-- 注意：实际使用时，需要在用户表中记录年级管理员所管理的年级信息
INSERT INTO data_query_permissions (role_id, data_type, query_scope, scope_value, field_permissions, can_export, can_print) 
SELECT r.id, 'student', 'grade', NULL, NULL, 0, 0
FROM roles r WHERE r.role_code = 'GRADE_ADMIN'
ON DUPLICATE KEY UPDATE query_scope=query_scope;

INSERT INTO data_query_permissions (role_id, data_type, query_scope, scope_value, field_permissions, can_export, can_print) 
SELECT r.id, 'score', 'grade', NULL, NULL, 0, 0
FROM roles r WHERE r.role_code = 'GRADE_ADMIN'
ON DUPLICATE KEY UPDATE query_scope=query_scope;

INSERT INTO data_query_permissions (role_id, data_type, query_scope, scope_value, field_permissions, can_export, can_print) 
SELECT r.id, 'emergency_contact', 'grade', NULL, NULL, 0, 0
FROM roles r WHERE r.role_code = 'GRADE_ADMIN'
ON DUPLICATE KEY UPDATE query_scope=query_scope;

-- 学生角色：仅可查询自己的信息
INSERT INTO data_query_permissions (role_id, data_type, query_scope, scope_value, field_permissions, can_export, can_print) 
SELECT r.id, 'student', 'own', NULL, NULL, 0, 0
FROM roles r WHERE r.role_code = 'STUDENT'
ON DUPLICATE KEY UPDATE query_scope=query_scope;

INSERT INTO data_query_permissions (role_id, data_type, query_scope, scope_value, field_permissions, can_export, can_print) 
SELECT r.id, 'score', 'own', NULL, NULL, 0, 0
FROM roles r WHERE r.role_code = 'STUDENT'
ON DUPLICATE KEY UPDATE query_scope=query_scope;

-- 后勤人员：可查看和管理来访登记
INSERT INTO data_query_permissions (role_id, data_type, query_scope, scope_value, field_permissions, can_export, can_print) 
SELECT r.id, 'visitor', 'all', NULL, NULL, 1, 1
FROM roles r WHERE r.role_code = 'LOGISTICS'
ON DUPLICATE KEY UPDATE query_scope=query_scope;

