-- ============================================
-- 创建campuser用户并授权
-- 需要在MySQL服务器上使用root用户执行
-- ============================================

-- 创建用户（如果不存在）
CREATE USER IF NOT EXISTS 'campuser'@'%' IDENTIFIED BY 'camp123456';

-- 授予camp数据库的所有权限
GRANT ALL PRIVILEGES ON camp.* TO 'campuser'@'%';

-- 刷新权限
FLUSH PRIVILEGES;

-- 查看用户权限
SHOW GRANTS FOR 'campuser'@'%';

-- 说明：
-- 1. 如果只需要特定权限，可以细化授权，例如：
--    GRANT SELECT, INSERT, UPDATE, DELETE ON camp.* TO 'campuser'@'%';
-- 
-- 2. 如果只允许特定IP连接，可以限制host，例如：
--    CREATE USER 'campuser'@'150.158.115.149' IDENTIFIED BY 'camp123456';
--    GRANT ALL PRIVILEGES ON camp.* TO 'campuser'@'150.158.115.149';
--
-- 3. 如果用户已存在，需要先删除：
--    DROP USER IF EXISTS 'campuser'@'%';

