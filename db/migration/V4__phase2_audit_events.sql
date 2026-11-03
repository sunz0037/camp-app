-- =============================================================================
-- 二期审计事件码放宽到 1–11
-- 文件：db/migration/V4__phase2_audit_events.sql
--
-- 不改 V3 的表。只放宽 sys_audit_logs.event_code 的检查，给写路径审计用：
-- 7 USER_ACCOUNT 8 COURSE_WRITE 9 CLASS_COURSE_WRITE 10 ATTENDANCE_WRITE 11 ANNOUNCEMENT_WRITE
-- 可重复执行。H2 不跑本文件，对应检查已写进 db/h2/schema.sql。
-- =============================================================================

SET NAMES utf8mb4;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'sys_audit_logs' AND constraint_name = 'chk_sys_audit_event') > 0,
  'ALTER TABLE sys_audit_logs DROP CHECK chk_sys_audit_event',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE sys_audit_logs
  ADD CONSTRAINT chk_sys_audit_event CHECK (event_code BETWEEN 1 AND 11);

ALTER TABLE sys_audit_logs
  MODIFY event_code TINYINT NOT NULL COMMENT '1 LOGIN_FAIL 2 PASSWORD_RESET 3 STUDENT_IMPORT 4 STUDENT_EXPORT 5 STUDENT_STATUS_CHANGE 6 STUDENT_PROFILE_CHANGE 7 USER_ACCOUNT 8 COURSE_WRITE 9 CLASS_COURSE_WRITE 10 ATTENDANCE_WRITE 11 ANNOUNCEMENT_WRITE';
