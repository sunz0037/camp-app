# 首期库表说明

建表与演示数据以仓库根目录为准：

- `db/migration/V1__phase1_schema.sql`
- `db/seed/V1__phase1_demo.sql`
- 执行顺序见 `db/README.md`

`src/main/resources/db/schema.sql` 及同目录其它脚本是旧的六角色 / 考试 / 访客全量，不作为首期权威 DDL。

## 和旧模型的差异

- 角色四档：`ADMIN`、`ACADEMIC`、`TEACHER`、`STUDENT`。家长使用学生账号，不建家长绑定。
- 数据范围用 `sys_teacher_classes`、`sys_affairs_grades`，不用 `scope_json`。
- 课表是 `class_course_sessions`（`weekday`、`period_start`、`period_end`、`room_no`）。
- 考勤汇总 `class_attendance_daily` 按班级和日期，不挂 `course_id`。明细 `student_attendance_records.course_id` 可空。
- 公告 `scope_type` + `scope_ids`，已读表 `announcement_reads` 对 `(announcement_id, user_id)` 唯一。
- 不包含成绩表、访客表。
