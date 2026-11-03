# 首期数据库

脚本都在仓库根目录 `db/`，不依赖远程库。本地 MySQL 8 执行即可。

成绩、访客不在首期必跑路径里。旧文件 `src/main/resources/db/schema.sql` 仍保留，只作历史对照；里面的 `exam_scores`、`visitor_registrations` 是 phase:2。

## 执行顺序

### 空库（新环境）

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS camp DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p camp < db/migration/V1__phase1_schema.sql
mysql -u root -p camp < db/seed/phase1_demo.sql
```

不要对空库只跑 V2。不要把 V1 跑在已经导入旧 `schema.sql` 的库上，V1 会直接失败并提示改跑 V2。

### 已有旧库

旧库是指执行过 `src/main/resources/db/schema.sql` 的库。`grades_and_classes.sql`、`class_courses.sql` 有则一并保留，没有也可以。

```bash
mysql -u root -p camp < db/migration/V2__phase1_legacy_align.sql
mysql -u root -p camp < db/seed/phase1_demo.sql
```

不要先跑 V1。V1 会再建一套 `oa_*` / `edu_*` 表，和旧表叠在一起。

V2 可以在 V1 之后再跑一遍，应当不改变已经对齐的结构。它按旧表改名和补列，不删除成绩表、访客表。

### 不要当作首期步骤

- `src/main/resources/db/exam_schedules.sql`
- `src/main/resources/db/schema.sql` 里的 `exam_scores`、`visitor_registrations`
- 根目录 `init_database.sql`、`execute_schema.sh`（仍指向旧 `schema.sql`）

`data_query_permissions.scope_value` 不是首期主授权。主授权是 `sys_teacher_classes`、`sys_affairs_grades`。

## 造数

`db/seed/phase1_demo.sql` 可重复执行。账号在 `sys_users`，密码都是 `Demo@123456`。这里只放哈希，没有首次激活或密码重置数据。

| 用户名 | 角色 | 说明 |
| --- | --- | --- |
| admin | ADMIN | 全校 |
| academic01 | ACADEMIC | 只授权初一年级 |
| teacher01 | TEACHER | 班主任，只授权初一1班 |
| student01 | STUDENT | 学号 S20260001，初一1班 |

另外有：初一1班 / 初一2班、数学课 2026-2027 学年第 1 学期的两条节次、2026-11-03 一条考勤日报和一条迟到异常、一条发给初一1班的公告且 student01 已读。

家长若要登录，另建一个 `STUDENT` 账号即可，没有家长绑定表。演示脚本没有另造家长账号。

## 和 JPA 的关系

本目录只交付 SQL 和说明，不改业务 Java。账号表是 `sys_users`，当前实体仍指向 `users` 以及 `announcements`、`class_courses` 等旧表名。在 API 骨架把 `@Table` 指到新表之前，不要用 `spring.jpa.hibernate.ddl-auto=update` 对着这套库启动，否则 Hibernate 会按旧实体把列加回去。

表名、唯一键和枚举见 [project_doc/db/PHASE1_SCHEMA.md](../project_doc/db/PHASE1_SCHEMA.md)。
