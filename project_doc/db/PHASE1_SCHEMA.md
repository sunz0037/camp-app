# 首期数据库（phase 1）

空库与旧库的执行顺序见仓库根目录 [db/README.md](../../db/README.md)。本地 H2 用 `db/h2/schema.sql` 与 `db/h2/seed.sql`，表结构与本页相同。

账号只做底座：`sys_users` 存登录名和密码哈希，`roles` 存四档角色。没有密码重置表、没有首次激活字段，造数也不覆盖这些流程。`sys_audit_logs` 仍预留密码重置事件码，供以后写入，本批不造这类数据。

二期增量（密码策略列、考试、访客、偏好、网盘）见 [PHASE2_SCHEMA.md](./PHASE2_SCHEMA.md)。旧脚本里的 `exam_scores`、`exam_schedules`、`visitor_registrations` 不作为二期结构。

## 表清单

| 表 | 作用 | 唯一键 |
| --- | --- | --- |
| `sys_users` | 账号底座。旧库由 `users` 改名 | `uk_username (username)` |
| `roles` | 四档角色 | `uk_role_code (role_code)` |
| `permissions` | 按钮权限码 | `uk_permission_code (permission_code)` |
| `role_permissions` | 角色-权限 | `uk_role_permission (role_id, permission_id)` |
| `user_roles` | 用户-角色 | `uk_user_role (user_id, role_id)` |
| `grades` | 年级 | `uk_grade_code (grade_code)` |
| `classes` | 班级 | `uk_class_code (class_code)`，另有 `(grade_id, class_number)` |
| `students` | 学籍，含 `class_id` / `grade_id` / `is_deleted` | `uk_student_no (student_no)`，`uk_student_user_id (user_id)` |
| `emergency_contacts` | 应急联系人 | 无自然键；家长登录不建绑定表 |
| `courses` | 课程库。`semester` / `academic_year` 不是排课来源 | `uk_course_code (course_code)` |
| `edu_class_courses` | 班级授课。旧表 `class_courses` | `uk_class_course_term (class_id, course_id, academic_year, semester)` |
| `edu_class_course_sessions` | 节次 | `uk_class_course_session (class_course_id, weekday, period_start, period_end)` |
| `edu_attendance_summaries` | 班级日报，不挂课程。旧表 `class_attendance_daily` | `uk_class_id_attendance_date (class_id, attendance_date)` |
| `edu_attendance_records` | 异常明细，`course_id` 可空。旧表 `student_attendance_records` | `uk_student_date_course (student_id, attendance_date, course_key)` |
| `sys_teacher_classes` | 教师可访问班级。旧表 `teacher_classes` | `uk_teacher_id_class_id (teacher_id, class_id)` |
| `sys_affairs_grades` | 教务可访问年级。旧表 `grade_admin_grades` | `uk_affairs_user_grade (user_id, grade_id)` |
| `oa_announcements` | 公告。旧表 `announcements` | 无自然键 |
| `oa_announcement_scopes` | 受众 | `uk_announcement_scope (announcement_id, scope_type, scope_id)` |
| `oa_announcement_reads` | 已读 | `uk_announcement_user (announcement_id, user_id)` |
| `sys_audit_logs` | 审计 | 无自然键 |

`course_key` 是生成列 `IFNULL(course_id, 0)`，只用于唯一约束。接口仍使用可空的 `courseId`。

旧库里 `username`、`student_no` 的唯一索引可能仍以列名命名。唯一性已经在，V2 不改索引名。新库使用 `uk_username`、`uk_student_no`。

旧库如果有无法回填的 `class_id` / `grade_id`，V2 会留下空值，不强行改成 `NOT NULL`。新库这些列按上表约束。

`data_query_permissions.scope_value` 不是主授权。

## 旧表改名

| 旧表 | 新表 |
| --- | --- |
| `users` | `sys_users` |
| `class_courses` | `edu_class_courses` |
| `class_attendance_daily` | `edu_attendance_summaries` |
| `student_attendance_records` | `edu_attendance_records` |
| `teacher_classes` | `sys_teacher_classes` |
| `grade_admin_grades` | `sys_affairs_grades`（`admin_id` 改为 `user_id`） |
| `announcements` | `oa_announcements` |

旧库 `edu_class_courses.semester` 若原是字符串，会改成 tinyint，原值留在 `semester_legacy`。无法识别的学年写成 `0000-0000`。

旧考勤明细 `status` 迁入 `exception_type` 后删除：1 缺勤→5，2 迟到→1，3 请假且原因含病→4，3 其他请假→3，4 早退→2。

## 枚举

角色档位（`roles.role_level`，大者更高）：

| role_code | 含义 | level |
| --- | --- | --- |
| ADMIN | 全校 | 40 |
| ACADEMIC | 授权年级。代替原 GRADE_ADMIN | 30 |
| TEACHER | 授权班级 / 所授课程 | 20 |
| STUDENT | 本人。家长同此角色，无绑定表 | 10 |

`GRADE_ADMIN`、`LOGISTICS` 在旧库中停用，不作为首期角色。

| 字段 | 库内值 | 接口字符串 |
| --- | --- | --- |
| `sys_users.user_type` | 1 / 2 / 3 | 学生 / 教职工 / 管理员。授权以角色为准 |
| `sys_users.status` | 0 / 1 | 禁用 / 启用 |
| `students.status` | 0 / 1 / 2 / 3 | SUSPENDED / ENROLLED / GRADUATED / WITHDRAWN |
| `students.is_deleted` | 0 / 1 | 否 / 是 |
| `students.gender` | 0 / 1 | 女 / 男 |
| `edu_class_courses.semester` | 1 / 2 | 第一学期 / 第二学期。不用 `semester_id` |
| `edu_class_course_sessions.weekday` | 1–7 | 周一至周日 |
| `edu_attendance_records.exception_type` | 1 / 2 / 3 / 4 / 5 | LATE / EARLY_LEAVE / PERSONAL_LEAVE / SICK_LEAVE / ABSENT |
| `edu_attendance_records.confirm_status` | 1 / 2 / 3 | PENDING / CONFIRMED / CLEARED。默认 2 |
| `oa_announcement_scopes.scope_type` | 1 / 2 / 3 | ALL / GRADE / CLASS。ALL 的 `scope_id` 为 0 |
| `oa_announcements.status` | 0 / 1 / 2 | DRAFT / PUBLISHED / WITHDRAWN |
| `sys_audit_logs.event_code` | 1–6 | LOGIN_FAIL / PASSWORD_RESET / STUDENT_IMPORT / STUDENT_EXPORT / STUDENT_STATUS_CHANGE / STUDENT_PROFILE_CHANGE |
| `sys_audit_logs.result` | 0 / 1 | 失败 / 成功 |

`id_card`、`phone` 可以入库。列表和导出脱敏、审计里不写明文，都由应用层处理。

## 和契约字段的对照

| 契约 | 列 |
| --- | --- |
| `academicYear` + `semester` + `sessions[]` | `edu_class_courses.academic_year`、`semester`，子表 `weekday` / `period_start` / `period_end` / `room_no` |
| 考勤幂等 `classId + date` | `edu_attendance_summaries (class_id, attendance_date)` |
| 异常可带 `courseId` | `edu_attendance_records.course_id` 可空；汇总表无课程 |
| `scopeType + scopeIds` | `oa_announcement_scopes` 每个 scope 一行 |
| 已读 | `oa_announcement_reads (announcement_id, user_id)` |
| 教师班级、教务年级 | `sys_teacher_classes`、`sys_affairs_grades` |
| 登录失败、重置密码、导入导出、学籍变更 | `sys_audit_logs.event_code` |

接口路径仍是 `/api/v1/users`。库表名是 `sys_users`，两者不是同一个标识。
