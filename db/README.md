# 数据库脚本

脚本都在仓库根目录 `db/`。本地开发用 H2，不连远程 MySQL。需要和线上一致时再用本机 MySQL 8。

首期表名是 `sys_users`、`edu_*`、`oa_*`，学籍仍是 `students`。H2 脚本不改名。二期沿用这套前缀，增量在 `V3__phase2_schema.sql`。旧文件 `src/main/resources/db/schema.sql` 仍只作历史对照；里面的 `exam_scores`、`visitor_registrations` 不是二期表。空库的 V1 不建这两类旧表；旧库跑 V2 时也不会删掉已经存在的成绩表、访客表。

表说明见 [project_doc/db/PHASE1_SCHEMA.md](../project_doc/db/PHASE1_SCHEMA.md) 和 [project_doc/db/PHASE2_SCHEMA.md](../project_doc/db/PHASE2_SCHEMA.md)。

## 同源：migration 是权威，H2 只做转译

`db/h2/schema.sql` 和 `db/h2/seed.sql` 不是第二套设计，禁止在 H2 里单独加表、改字段或改唯一键。

1. 改表结构以 `db/migration/` 为权威（空库 `V1__phase1_schema.sql`，旧库 `V2__phase1_legacy_align.sql`，二期表 `V3__phase2_schema.sql`，审计事件码 `V4__phase2_audit_events.sql`）。
2. 每次改完 migration 或 `db/seed/`，同一提交里把结果转译到 `db/h2/`。H2 的 `schema.sql` 是空库执行 V1 再执行 V3 之后的全量，不是只含首期。
3. H2 只保留方言差异，不表达另一套模型。

允许的差异只有这些：

- 去掉 `ENGINE`、`CHARSET`、`utf8mb4`、`ON UPDATE CURRENT_TIMESTAMP`、`SET NAMES`、`PREPARE`
- 非唯一索引加上表名前缀（H2 索引名全库唯一）；`uk_*` 保持原名
- `course_key` 仍是 `IFNULL(course_id, 0)`，去掉 MySQL 的 `STORED`
- 造数里 `SET` 用 `=`，不用 `:=`；`UPDATE` 不用 `JOIN`

检查清单（改库时勾一遍）：

- [ ] 新表、新列、唯一键、枚举先写进 `db/migration`
- [ ] `db/h2/schema.sql` 已按上面的方言规则转译，没有多出来的表或字段
- [ ] 造数先改 `db/seed/phase1_demo.sql` 或 `db/seed/phase2_demo.sql`，再同步 `db/h2/seed.sql` 里对应的那一段
- [ ] 演示账号仍是 `Demo@123456` 的同一哈希

## 本地 H2

脚本：`db/h2/schema.sql`、`db/h2/seed.sql`。和 MySQL 版相比，去掉了 `ENGINE`、`utf8mb4`、`ON UPDATE CURRENT_TIMESTAMP`。布尔和首期枚举继续用 `TINYINT`，不用 `BOOLEAN`。访客状态和分享权限按二期脚本直接存字符串。自增仍是 `AUTO_INCREMENT`。非唯一索引加了表名前缀，因为 H2 的索引名全库唯一；`uk_*` 名字不变。`course_key` 仍是 `IFNULL(course_id, 0)`。二期表已经写进这两份脚本，启动方式不用改。

不要把 MySQL 的 `db/migration` 直接丢给 H2（有 `PREPARE`、库级不唯一的索引名）。`db/h2/` 与 MySQL 首期库表名相同。H2 没有旧库对齐脚本。本地一律空库执行 `db/h2/`。

在仓库根目录启动应用（会自己跑这两份脚本，`ddl-auto=none`）：

```bash
./gradlew bootRun --args='--spring.profiles.active=h2'
```

配置在 `src/main/resources/application-h2.properties`。连接串：

```text
jdbc:h2:mem:camp;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE
```

用户 `sa`，密码空。控制台 `/h2-console`。工作目录必须是仓库根，因为脚本用 `file:db/h2/`。

只建库、不启动应用时，用 H2 RunScript 指向同一 URL（内存库要在同一进程里跑完 schema 和 seed）。文件库示例：

```bash
java -cp ~/.gradle/caches/modules-2/files-2.1/com.h2database/h2/*/*/h2-*.jar org.h2.tools.RunScript \
  -url "jdbc:h2:file:./build/camp-h2;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE" \
  -user sa -script db/h2/schema.sql
java -cp ~/.gradle/caches/modules-2/files-2.1/com.h2database/h2/*/*/h2-*.jar org.h2.tools.RunScript \
  -url "jdbc:h2:file:./build/camp-h2;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE" \
  -user sa -script db/h2/seed.sql
```

## MySQL

### 空库（新环境）

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS camp DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p camp < db/migration/V1__phase1_schema.sql
mysql -u root -p camp < db/migration/V3__phase2_schema.sql
mysql -u root -p camp < db/seed/phase1_demo.sql
mysql -u root -p camp < db/seed/phase2_demo.sql
```

不要对空库只跑 V2 或只跑 V3。不要把 V1 跑在已经导入旧 `schema.sql` 的库上，V1 会直接失败并提示改跑 V2。V3 在缺少首期表时也会失败，并提示先跑 V1 或 V2。

### 已有旧库

旧库是指执行过 `src/main/resources/db/schema.sql` 的库。`grades_and_classes.sql`、`class_courses.sql` 有则一并保留，没有也可以。旧库只跑 V2，不要再跑 V1：

```bash
mysql -u root -p camp < db/migration/V2__phase1_legacy_align.sql
mysql -u root -p camp < db/migration/V3__phase2_schema.sql
mysql -u root -p camp < db/seed/phase1_demo.sql
mysql -u root -p camp < db/seed/phase2_demo.sql
```

不要先跑 V1。V1 会再建一套 `oa_*` / `edu_*` 表，和旧表叠在一起。

V2 可以在 V1 之后再跑一遍，应当不改变已经对齐的结构。它按旧表改名和补列，不删除旧成绩表、旧访客表。V3 在 V2 之后执行，新建 `edu_exams`、`oa_visitors` 等表，不改那两张旧表。V3 可以重复执行。

### 不要当作首期或二期步骤

- `src/main/resources/db/exam_schedules.sql`
- `src/main/resources/db/schema.sql` 里的 `exam_scores`、`visitor_registrations`（二期用 `edu_exams` / `edu_exam_scores` / `oa_visitors`）
- 根目录 `init_database.sql`、`execute_schema.sh`（仍指向旧 `schema.sql`）

`data_query_permissions.scope_value` 不是首期主授权。主授权是 `sys_teacher_classes`、`sys_affairs_grades`。

## 造数

MySQL 首期用 `db/seed/phase1_demo.sql`，二期用 `db/seed/phase2_demo.sql`。H2 把两段按同样顺序放进 `db/h2/seed.sql`，都可重复执行。账号在 `sys_users`，密码都是 `Demo@123456`。这里只放哈希，没有首次激活或密码重置流程。二期会给 `student01` 写一条密码历史。种子按用户名写入，不写死自增 ID。

| 用户名 | 角色 | 开发态 | 说明 |
| --- | --- | --- | --- |
| admin | ADMIN | `dev-ADMIN` | 全校。空库上常见 id 1 |
| academic01 | ACADEMIC | `dev-ACADEMIC` | 只授权初一年级。常见 id 2 |
| teacher01 | TEACHER | `dev-TEACHER` | 班主任，只授权初一1班。常见 id 3 |
| student01 | STUDENT | `dev-STUDENT` | 学号 S20260001，初一1班。常见 id 4 |

id 若不是这组，用请求头 `X-Dev-User-Id` 指到种子用户。`h2` profile 下可用 `Authorization: Bearer dev-*`。

另外有：初一1班 / 初一2班、数学课 2026-2027 学年第 1 学期的两条节次、2026-11-03 一条考勤日报和一条迟到异常、一条发给初一1班的公告且 student01 已读。

二期造数在首期之后：`student02`（学号 S20260002）、一场数学期中测验和两行成绩、两条访客、`student01` 的 `campus-blue` 偏好、教师网盘目录和两个文件（其中一个以 `READ` 分享给 student01）、一行密码策略（`history_size = 5`）。

家长若要登录，另建一个 `STUDENT` 账号即可，没有家长绑定表。演示脚本没有另造家长账号。

## 二期表与审计码

二期表以已合入的 `db/migration/V3__phase2_schema.sql` 为准：`edu_exams`、`edu_exam_scores`、`oa_visitors`、`oa_visitor_logs`、`oa_files`、`oa_file_shares`、`sys_user_preferences`、密码策略与密码历史。接口直接用这些表，不另建 `exam_schedules`、`exam_scores`、`sys_files`、`sys_user_settings`。

`db/migration/V4__phase2_audit_events.sql` 只把 `sys_audit_logs.event_code` 从 1–6 放宽到 1–11，不改 V3 的列。H2 的同一约束写在 `db/h2/schema.sql`，不要把 V4 直接丢给 H2。

## 和 JPA 的关系

`ddl-auto=none`。表由 SQL 脚本维护，Hibernate 不改表。H2 profile 执行 `db/h2/schema.sql` 与 `db/h2/seed.sql`。

实体 `@Table` 已与首期表名对齐：`User` → `sys_users`；公告 → `oa_announcements`、`oa_announcement_scopes`、`oa_announcement_reads`；排课 → `edu_class_courses`、`edu_class_course_sessions`；考勤 → `edu_attendance_summaries`、`edu_attendance_records`。学籍仍是 `students`，应急联系人仍是 `emergency_contacts`。

`Student` 的 `class_id` / `grade_id` 与 `classEntity` / `gradeEntity` 共用列，标量字段 `insertable=false, updatable=false`，避免 Hibernate 重复映射。`students.is_deleted` 仍映射为软删字段。

首期表名、唯一键和枚举见 [project_doc/db/PHASE1_SCHEMA.md](../project_doc/db/PHASE1_SCHEMA.md)。二期见 [project_doc/db/PHASE2_SCHEMA.md](../project_doc/db/PHASE2_SCHEMA.md)。
