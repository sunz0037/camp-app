-- ============================================
-- 校园OA系统数据库初始化脚本
-- 数据库：camp
-- 时区：北京时间（Asia/Shanghai）
-- ============================================

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS camp 
  DEFAULT CHARACTER SET utf8mb4 
  DEFAULT COLLATE utf8mb4_unicode_ci;

-- 使用camp数据库
USE camp;

-- 设置时区为北京时间
SET time_zone = '+08:00';

-- 执行建表语句（从schema.sql复制）
-- 注意：这里只包含建表语句，完整语句请参考 schema.sql 文件

