-- ============================================
-- phase:2，不是首期必跑脚本。首期入口见仓库根目录 db/README.md。
-- ============================================
-- 考试安排表
-- ============================================
CREATE TABLE IF NOT EXISTS exam_schedules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '考试安排ID',
    exam_name VARCHAR(100) NOT NULL COMMENT '考试名称',
    exam_date DATETIME NOT NULL COMMENT '考试时间',
    target_grade VARCHAR(20) NOT NULL COMMENT '考试面向年级（如：小学1年级、初中1年级、高中1年级）',
    exam_type VARCHAR(20) NOT NULL COMMENT '考试类型：月考、期中、期末',
    is_public TINYINT NOT NULL DEFAULT 0 COMMENT '是否公开：0-不公开（不向家长公布成绩），1-公开（向家长公布成绩）',
    remark TEXT COMMENT '备注',
    created_by BIGINT NOT NULL COMMENT '创建人ID（教工）',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-已取消，1-正常',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT,
    INDEX idx_exam_date (exam_date),
    INDEX idx_target_grade (target_grade),
    INDEX idx_exam_type (exam_type),
    INDEX idx_created_by (created_by),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试安排表（教工可创建考试安排）';

-- ============================================
-- 修改考试成绩表，添加关联考试安排ID和备注字段
-- ============================================
ALTER TABLE exam_scores 
ADD COLUMN exam_schedule_id BIGINT COMMENT '关联考试安排ID' AFTER course_id,
ADD COLUMN remark_detail TEXT COMMENT '备注详情（记录如考试缺席等原因）' AFTER remark,
ADD FOREIGN KEY (exam_schedule_id) REFERENCES exam_schedules(id) ON DELETE SET NULL,
ADD INDEX idx_exam_schedule_id (exam_schedule_id);

