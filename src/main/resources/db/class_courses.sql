-- ============================================
-- 班级授课科目关联表
-- ============================================

-- 班级-科目关联表（维护班级与授课科目的关系）
CREATE TABLE IF NOT EXISTS class_courses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
    class_id BIGINT NOT NULL COMMENT '班级ID',
    course_id BIGINT NOT NULL COMMENT '科目ID',
    teacher_id BIGINT COMMENT '授课教师ID（可选，如果科目表已有教师则可为空）',
    semester VARCHAR(20) COMMENT '学期（可选，用于区分不同学期的课程安排）',
    academic_year VARCHAR(20) COMMENT '学年（可选）',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-停用，1-启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    FOREIGN KEY (teacher_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_class_id (class_id),
    INDEX idx_course_id (course_id),
    INDEX idx_teacher_id (teacher_id),
    INDEX idx_status (status),
    UNIQUE KEY uk_class_course (class_id, course_id, semester, academic_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级授课科目关联表';

