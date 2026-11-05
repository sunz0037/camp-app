# 首期库表说明

建表与演示数据以数据库 PR 的仓根脚本为准：

- `db/migration/V1__phase1_schema.sql`：空库
- `db/migration/V2__phase1_legacy_align.sql`：旧库对齐
- `db/seed/phase1_demo.sql`：演示数据，密码 `Demo@123456`

执行顺序见 `db/README.md`。应用使用 `ddl-auto=none`，不另起一套表名。

## 表名

账号 `sys_users`。授课与考勤 `edu_class_courses`、`edu_class_course_sessions`、`edu_attendance_summaries`、`edu_attendance_records`。公告 `oa_announcements`、`oa_announcement_scopes`、`oa_announcement_reads`。审计 `sys_audit_logs`。数据范围 `sys_teacher_classes`、`sys_affairs_grades`。

年级、班级、学生、课程库、角色仍是 `grades`、`classes`、`students`、`courses`、`roles`。

`src/main/resources/db/schema.sql` 是旧的无前缀全量，不是首期入口。
