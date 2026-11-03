package com.camp.app.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class H2Phase1ScriptsTest {
    @Test
    void stripsMysqlPrepareAndRewritesGeneratedColumn() {
        String adapted = H2Phase1Scripts.adapt("""
                SET NAMES utf8mb4;
                SET @sql := IF(1 > 0, 'SELECT 1', 'SELECT 2');
                PREPARE stmt FROM @sql;
                EXECUTE stmt;
                DEALLOCATE PREPARE stmt;
                CREATE TABLE demo (
                  id BIGINT PRIMARY KEY,
                  course_key BIGINT AS (IFNULL(course_id, 0)) STORED
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """);
        assertFalse(adapted.toLowerCase().contains("prepare"));
        assertTrue(adapted.contains("GENERATED ALWAYS AS"));
        assertTrue(adapted.contains("CREATE TABLE demo"));
        assertFalse(adapted.contains("ENGINE"));
        String indexed = H2Phase1Scripts.adapt("""
                CREATE TABLE IF NOT EXISTS roles (
                  status TINYINT,
                  UNIQUE KEY uk_role_code (role_code),
                  KEY idx_status (status)
                );
                CREATE TABLE IF NOT EXISTS courses (
                  status TINYINT,
                  KEY idx_status (status)
                );
                """);
        assertTrue(indexed.contains("KEY roles_idx_status"));
        assertTrue(indexed.contains("KEY courses_idx_status"));
        assertTrue(indexed.contains("UNIQUE KEY roles_uk_role_code"));
    }
}
