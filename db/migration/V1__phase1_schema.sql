-- 校园 OA 首期建表（Phase 1）
-- 不含考试成绩、访客登记。旧脚本仍在 src/main/resources/db/，不是本文件的替代品。
-- 空库按 db/README.md 的顺序执行。已有旧表时 CREATE IF NOT EXISTS 不会补列。

CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(50) NOT NULL,
    id_card VARCHAR(18) NULL,
    phone VARCHAR(20) NULL,
    email VARCHAR(100) NULL,
    user_type TINYINT NOT NULL DEFAULT 1 COMMENT '1学生 2教职工 3管理员',
    status TINYINT NOT NULL DEFAULT 1,
    must_change_password TINYINT NULL DEFAULT 0,
    avatar_url VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code VARCHAR(50) NOT NULL UNIQUE,
    role_name VARCHAR(50) NOT NULL,
    description VARCHAR(255) NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS role_permission_grants (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code VARCHAR(50) NOT NULL,
    permission_code VARCHAR(100) NOT NULL,
    UNIQUE KEY uk_role_perm_code (role_code, permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS grades (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    grade_code VARCHAR(20) NOT NULL UNIQUE,
    grade_name VARCHAR(50) NOT NULL,
    grade_type VARCHAR(20) NOT NULL,
    grade_level INT NOT NULL,
    description VARCHAR(255) NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS classes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    grade_id BIGINT NOT NULL,
    class_code VARCHAR(50) NOT NULL,
    class_name VARCHAR(50) NOT NULL,
    class_number INT NOT NULL,
    capacity INT NULL DEFAULT 50,
    current_count INT NULL DEFAULT 0,
    head_teacher_id BIGINT NULL,
    description VARCHAR(255) NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_grade_class (grade_id, class_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS students (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    student_no VARCHAR(20) NOT NULL UNIQUE,
    grade VARCHAR(20) NULL,
    grade_id BIGINT NULL,
    class_name VARCHAR(50) NULL,
    class_id BIGINT NULL,
    major VARCHAR(100) NULL,
    department VARCHAR(100) NULL,
    enrollment_date DATE NULL,
    graduation_date DATE NULL,
    dormitory VARCHAR(50) NULL,
    address VARCHAR(255) NULL,
    political_status VARCHAR(20) NULL,
    nationality VARCHAR(50) NULL DEFAULT '中国',
    gender TINYINT NULL,
    birth_date DATE NULL,
    status TINYINT NOT NULL DEFAULT 1,
    is_deleted TINYINT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS emergency_contacts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    contact_name VARCHAR(50) NOT NULL,
    relationship VARCHAR(20) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    alternate_phone VARCHAR(20) NULL,
    address VARCHAR(255) NULL,
    is_primary TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_emergency_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS courses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_code VARCHAR(20) NOT NULL UNIQUE,
    course_name VARCHAR(100) NOT NULL,
    credit DECIMAL(3,1) NOT NULL,
    course_type VARCHAR(20) NULL,
    department VARCHAR(100) NULL,
    teacher_id BIGINT NULL,
    semester VARCHAR(20) NULL,
    academic_year VARCHAR(20) NULL,
    description TEXT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS class_courses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    class_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    teacher_id BIGINT NULL,
    semester VARCHAR(20) NULL,
    academic_year VARCHAR(20) NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_class_course (class_id, course_id, semester, academic_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS class_course_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    class_course_id BIGINT NOT NULL,
    weekday INT NOT NULL COMMENT '1-7，周一为1',
    period_start INT NOT NULL,
    period_end INT NOT NULL,
    room_no VARCHAR(50) NULL,
    INDEX idx_session_class_course (class_course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_teacher_classes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    teacher_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_teacher_class (teacher_id, class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_affairs_grades (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    grade_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_affairs_grade (user_id, grade_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS class_attendance_daily (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    attendance_date DATE NOT NULL,
    grade VARCHAR(20) NOT NULL,
    class_name VARCHAR(50) NOT NULL,
    class_id BIGINT NULL,
    expected_count INT NOT NULL,
    actual_count INT NOT NULL,
    absent_count INT NOT NULL,
    late_count INT NOT NULL,
    leave_count INT NOT NULL,
    early_leave_count INT NULL,
    recorded_by BIGINT NULL,
    remark VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_class_date_name (attendance_date, grade, class_name),
    INDEX idx_attendance_class_date (class_id, attendance_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS student_attendance_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    class_id BIGINT NULL,
    summary_id BIGINT NULL,
    course_id BIGINT NULL,
    attendance_date DATE NOT NULL,
    status TINYINT NOT NULL,
    exception_type VARCHAR(30) NULL,
    confirm_status VARCHAR(20) NULL,
    reason_type VARCHAR(50) NULL,
    reason_description VARCHAR(500) NULL,
    parent_notified TINYINT NULL,
    parent_notify_time DATETIME NULL,
    parent_confirm_time DATETIME NULL,
    attachment_url VARCHAR(500) NULL,
    recorded_by BIGINT NULL,
    remark VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_student_date (student_id, attendance_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS announcements (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    category VARCHAR(50) NULL,
    publisher_id BIGINT NOT NULL,
    publish_time DATETIME NOT NULL,
    expire_time DATETIME NULL,
    is_top TINYINT NOT NULL DEFAULT 0,
    is_urgent TINYINT NOT NULL DEFAULT 0,
    target_audience VARCHAR(50) NULL,
    scope_type VARCHAR(20) NULL,
    scope_ids VARCHAR(1000) NULL,
    view_count INT NOT NULL DEFAULT 0,
    attachment_url VARCHAR(500) NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS announcement_reads (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    announcement_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    read_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_announcement_user (announcement_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS auth_refresh_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    jti VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expires_at DATETIME NOT NULL,
    revoked TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_refresh_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS stored_files (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    original_name VARCHAR(255) NOT NULL,
    stored_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NULL,
    size_bytes BIGINT NOT NULL,
    uploader_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    action VARCHAR(50) NOT NULL,
    actor_id BIGINT NULL,
    target_type VARCHAR(50) NULL,
    target_id VARCHAR(64) NULL,
    detail VARCHAR(500) NULL,
    success TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_action (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
