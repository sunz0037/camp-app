-- ============================================
-- 年级和班级信息维护表
-- ============================================

-- 年级表
CREATE TABLE IF NOT EXISTS grades (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '年级ID',
    grade_code VARCHAR(20) NOT NULL UNIQUE COMMENT '年级代码（如：PRIMARY_1, JUNIOR_1, SENIOR_1）',
    grade_name VARCHAR(50) NOT NULL COMMENT '年级名称（如：小学一年级、初中一年级、高中一年级）',
    grade_type VARCHAR(20) NOT NULL COMMENT '年级类型：PRIMARY-小学，JUNIOR-初中，SENIOR-高中',
    grade_level INT NOT NULL COMMENT '年级级别：1-6（小学），1-3（初中），1-3（高中）',
    description VARCHAR(255) COMMENT '年级描述',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-停用，1-启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_grade_type (grade_type),
    INDEX idx_grade_level (grade_level),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='年级表';

-- 班级表
CREATE TABLE IF NOT EXISTS classes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '班级ID',
    grade_id BIGINT NOT NULL COMMENT '年级ID（外键）',
    class_code VARCHAR(50) NOT NULL COMMENT '班级代码（如：PRIMARY_1_CLASS_1）',
    class_name VARCHAR(50) NOT NULL COMMENT '班级名称（如：一年级1班）',
    class_number INT NOT NULL COMMENT '班级序号（如：1班、2班）',
    capacity INT DEFAULT 50 COMMENT '班级容量（默认50人）',
    current_count INT DEFAULT 0 COMMENT '当前学生数',
    head_teacher_id BIGINT COMMENT '班主任ID（外键）',
    description VARCHAR(255) COMMENT '班级描述',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-停用，1-启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (grade_id) REFERENCES grades(id) ON DELETE RESTRICT,
    FOREIGN KEY (head_teacher_id) REFERENCES users(id) ON DELETE SET NULL,
    UNIQUE KEY uk_grade_class (grade_id, class_number),
    INDEX idx_grade_id (grade_id),
    INDEX idx_class_code (class_code),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级表';

-- 初始化年级数据（小学1-6年级，初中1-3年级，高中1-3年级）
INSERT INTO grades (grade_code, grade_name, grade_type, grade_level, description, status) VALUES
-- 小学
('PRIMARY_1', '小学一年级', 'PRIMARY', 1, '小学一年级', 1),
('PRIMARY_2', '小学二年级', 'PRIMARY', 2, '小学二年级', 1),
('PRIMARY_3', '小学三年级', 'PRIMARY', 3, '小学三年级', 1),
('PRIMARY_4', '小学四年级', 'PRIMARY', 4, '小学四年级', 1),
('PRIMARY_5', '小学五年级', 'PRIMARY', 5, '小学五年级', 1),
('PRIMARY_6', '小学六年级', 'PRIMARY', 6, '小学六年级', 1),
-- 初中
('JUNIOR_1', '初中一年级', 'JUNIOR', 1, '初中一年级', 1),
('JUNIOR_2', '初中二年级', 'JUNIOR', 2, '初中二年级', 1),
('JUNIOR_3', '初中三年级', 'JUNIOR', 3, '初中三年级', 1),
-- 高中
('SENIOR_1', '高中一年级', 'SENIOR', 1, '高中一年级', 1),
('SENIOR_2', '高中二年级', 'SENIOR', 2, '高中二年级', 1),
('SENIOR_3', '高中三年级', 'SENIOR', 3, '高中三年级', 1)
ON DUPLICATE KEY UPDATE grade_name = VALUES(grade_name);

