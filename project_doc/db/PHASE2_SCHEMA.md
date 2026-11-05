# 二期数据库（phase 2）

只增加表和 `sys_users` 上的密码策略列。不改首期列的含义，不改业务 Java。执行顺序和 H2 启动方式见 [db/README.md](../../db/README.md)。

## 合入注意

PR #2（`cursor/phase1-schema-align-208a`）尚未进入默认分支时，本增量从该分支的 tip 拉出。请先合 #2，再合本增量。空库顺序是 V1 → V3；已经跑过旧 `schema.sql` 的库是 V2 → V3。不要跳过 V1/V2 直接跑 V3，脚本会失败并提示。

V3 可重复执行。已有的列和表会跳过，不删除业务行。`sys_password_policy` 的默认行只在表里还没有 `id = 1` 时插入，重复执行不覆盖已改过的策略。

H2 启动命令不变，仍然是：

```bash
./gradlew bootRun --args='--spring.profiles.active=h2'
```

`db/h2/schema.sql` 是 V1 加 V3 的结果，`db/h2/seed.sql` 是首期造数加 `db/seed/phase2_demo.sql` 的转译。H2 不执行 `db/migration/V3__phase2_schema.sql`。

## 和附件的对应

| 附件 | 库内 |
| --- | --- |
| `term` | `academic_year` + `semester`（1 / 2），与 `edu_class_courses` 相同 |
| `path/storage_key` | 只存 `storage_key`。目录层级用 `parent_id` + `name`，不另存一份 path |
| `share_to_role` | `share_to_role_id`，外键到 `roles.id`。和 `share_to_user_id` 二选一 |
| 访客 `status`、分享 `permission` | 直接存附件里的字符串，不用新的数字编码 |

旧脚本里的 `exam_scores`、`exam_schedules`、`visitor_registrations` 不是这套表。V3 不改它们，也不把旧数据迁进来。

## 表清单

| 表 | 作用 | 唯一键 | 外键 |
| --- | --- | --- | --- |
| `sys_users`（增量列） | 改密时间、到期、失败次数、锁定、必须改密 | 仍是 `uk_username` | 无新外键 |
| `sys_password_policy` | 全库一行密码策略 | 主键 `id`，且 `id = 1` | 无 |
| `sys_password_history` | 历史密码哈希 | 无自然键 | `user_id` → `sys_users.id` |
| `sys_user_preferences` | 主题 key、语言、扩展 JSON | `uk_pref_user (user_id)` | `user_id` → `sys_users.id` |
| `edu_exams` | 班级考试 | `uk_edu_exam_natural (class_id, course_id, exam_date, title)` | `course_id` → `courses.id`；`class_id` → `classes.id`；`created_by` → `sys_users.id` |
| `edu_exam_scores` | 成绩 | `uk_exam_student (exam_id, student_id)` | `exam_id` → `edu_exams.id`；`student_id` → `students.id` |
| `oa_visitors` | 访客登记 | 无自然键 | `host_user_id`、`created_by` → `sys_users.id` |
| `oa_visitor_logs` | 访客流水，只追加 | 无自然键 | `visitor_id` → `oa_visitors.id`；`operator_id` → `sys_users.id` |
| `oa_files` | 网盘元数据 | `uk_storage_key (storage_key)` | `owner_id` → `sys_users.id`；`parent_id` → `oa_files.id` |
| `oa_file_shares` | 分享给用户或角色 | `uk_file_share_user (file_id, share_to_user_id)`；`uk_file_share_role (file_id, share_to_role_id)` | `file_id` → `oa_files.id`；`share_to_user_id` → `sys_users.id`；`share_to_role_id` → `roles.id` |

`edu_exams` 上有 `course_id`、`class_id`、`exam_date` 索引。`edu_exam_scores` 上有 `exam_id`、`student_id` 索引。`oa_visitors` 上有 `status`、`host_user_id`、`expected_arrive` 索引。`oa_files` 上有 `(owner_id, parent_id)` 索引。

成绩表和分享表多了 `created_at` / `updated_at`，分享目标列名为 `share_to_role_id`。这些是为了和首期表的时间戳、外键写法对齐。

## sys_users 增量列

| 列 | 空值 | 含义 |
| --- | --- | --- |
| `password_changed_at` | 可空 | 最近一次成功改密的时间 |
| `password_expires_at` | 可空 | 单独写下的到期时间 |
| `failed_login_count` | 非空，默认 0 | 连续失败次数。已有账号补 0 |
| `locked_until` | 可空 | 锁定截止，空表示未锁定 |
| `must_change_password` | 非空，默认 0 | 0 否，1 是。已有账号补 0，不会被强制改密 |

两列计数带默认值，这样旧行不用逐条回填，首期原有列的含义不变。没有密码重置表，也没有首次激活字段。

## 密码策略与历史

`sys_password_policy` 只有 `id = 1` 这一行。默认值：最短 8 位，大写、小写、数字、特殊字符都要，`history_size = 5`，连续失败 5 次锁定 30 分钟，`expire_days = 90`。

应用层改密时把新哈希写入 `sys_password_history`，并只保留该用户最近 **N = 5** 行（与 `history_size` 一致）。数据库不自动删旧哈希。

## 枚举

| 字段 | 库内值 |
| --- | --- |
| `must_change_password`、`require_*`、`is_dir` | 0 / 1 |
| `edu_exams.semester` | 1 第一学期，2 第二学期 |
| `oa_visitors.status` | `PENDING` / `APPROVED` / `REJECTED` / `CHECKED_IN` / `CHECKED_OUT` / `CANCELLED` |
| `oa_file_shares.permission` | `READ` / `WRITE` |
| `sys_user_preferences.theme_key` | 自由字符串。演示值为 `default`、`dark`、`campus-blue`。不存色板 hex |

`oa_visitor_logs.action` 是字符串，演示用 `SUBMIT` 和 `APPROVE`。不做成检查约束，避免每加一种动作都要改表。

`phone`、`id_no`、`password_hash` 可以入库。列表脱敏和审计不写明文仍由应用层处理。演示造数不写证件号。

## 演示数据

`db/seed/phase2_demo.sql` 在首期造数之后执行，至少包含：

- 密码策略默认行（已存在则跳过）
- `student02` / 学号 `S20260002`（李小雨，初一1班），以及 `S20260001` 92.50、`S20260002` 78.00 两行成绩，考试为 2026-11-10 数学期中测验
- 两条访客：张来访（`APPROVED`）、陈家长（`PENDING`），各一条流水
- `student01` 一条偏好：`campus-blue` / `zh-CN`
- 教师 `teacher01` 的目录「数学资料」和两个文件，其中说明文件以 `READ` 分享给 `student01`
- `student01` 的一条密码历史（当前演示哈希）

账号密码仍是 `Demo@123456`。

## 非目标

不做 JWT、对象存储、换肤前端、考试或访客 API，也不改首期已有列的含义。
