-- 密码重置日志表
CREATE TABLE IF NOT EXISTS password_reset_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    user_id BIGINT COMMENT '用户ID（成功时记录）',
    identifier VARCHAR(50) COMMENT '学号/工号',
    identity_type TINYINT COMMENT '身份类型：1-学生，2-教工',
    reset_type TINYINT NOT NULL DEFAULT 1 COMMENT '重置类型：1-学生六要素，2-教工手机验证码',
    status TINYINT NOT NULL COMMENT '状态：0-失败，1-成功',
    failure_reason VARCHAR(255) COMMENT '失败原因',
    ip_address VARCHAR(50) COMMENT '请求IP',
    user_agent VARCHAR(255) COMMENT '用户代理',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_identifier (identifier),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_identifier_created (identifier, created_at),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='密码重置日志表';

