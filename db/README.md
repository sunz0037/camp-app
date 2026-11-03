# 首期数据库

脚本都在仓库根目录 `db/`。本地开发用 H2，不连远程 MySQL。需要和线上一致时再用本机 MySQL 8。

成绩、访客不在首期必跑路径里。旧文件 `src/main/resources/db/schema.sql` 仍保留，只作历史对照；里面的 `exam_scores`、`visitor_registrations` 是 phase:2。

表名与字段以 phase1 为准（`sys_users`、`edu_*`、`oa_*`，学籍仍是 `students`）。H2 脚本不改名。

## 同源：migration 是权威，H2 只做转译

`db/h2/schema.sql` 和 `db/h2/seed.sql` 不是第二套设计，禁止在 H2 里单独加表、改字段或改唯一键。

1. 改表结构以 `db/migration/` 为权威（当前是 `V1__phase1_schema.sql`，旧库增量是 `V2__phase1_legacy_align.sql`）。
2. 每次改完 migration 或 `db/seed/phase1_demo.sql`，同一提交里把结果转译到 `db/h2/`。
3. H2 只保留方言差异，不表达另一套模型。

允许的差异只有这些：

- 去掉 `ENGINE`、`CHARSET`、`utf8mb4`、`ON UPDATE CURRENT_TIMESTAMP`、`SET NAMES`、`PREPARE`
- 非唯一索引加上表名前缀（H2 索引名全库唯一）；`uk_*` 保持原名
- `course_key` 仍是 `IFNULL(course_id, 0)`，去掉 MySQL 的 `STORED`
- 造数里 `SET` 用 `=`，不用 `:=`；`UPDATE` 不用 `JOIN`

检查清单（改库时勾一遍）：

- [ ] 新表、新列、唯一键、枚举先写进 `db/migration`
- [ ] `db/h2/schema.sql` 已按上面的方言规则转译，没有多出来的表或字段
- [ ] 造数先改 `db/seed/phase1_demo.sql`，再同步 `db/h2/seed.sql`
- [ ] 演示账号仍是 `Demo@123456` 的同一哈希

## 本地 H2

脚本：`db/h2/schema.sql`、`db/h2/seed.sql`。和 MySQL 版相比，去掉了 `ENGINE`、`utf8mb4`、`ON UPDATE CURRENT_TIMESTAMP`。枚举继续用 `TINYINT`，不用 `BOOLEAN`。自增仍是 `AUTO_INCREMENT`。非唯一索引加了表名前缀，因为 H2 的索引名全库唯一；`uk_*` 名字不变。`course_key` 仍是 `IFNULL(course_id, 0)`。

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

H2 没有旧库对齐脚本。本地一律空库执行 `db/h2/`。

## MySQL

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

MySQL 用 `db/seed/phase1_demo.sql`，H2 用 `db/h2/seed.sql`，内容相同，都可重复执行。账号在 `sys_users`，密码都是 `Demo@123456`。这里只放哈希，没有首次激活或密码重置数据。

| 用户名 | 角色 | 说明 |
| --- | --- | --- |
| admin | ADMIN | 全校 |
| academic01 | ACADEMIC | 只授权初一年级 |
| teacher01 | TEACHER | 班主任，只授权初一1班 |
| student01 | STUDENT | 学号 S20260001，初一1班 |

另外有：初一1班 / 初一2班、数学课 2026-2027 学年第 1 学期的两条节次、2026-11-03 一条考勤日报和一条迟到异常、一条发给初一1班的公告且 student01 已读。

家长若要登录，另建一个 `STUDENT` 账号即可，没有家长绑定表。演示脚本没有另造家长账号。

## 和 JPA 的关系

`User` 已指向 `sys_users`。`Student` 的 `class_id` / `grade_id` 只保留关联字段可写，避免 Hibernate 重复映射导致起不来。公告、排课、考勤等实体仍指向旧表名。在 API 骨架改完 `@Table` 之前，不要用 `ddl-auto=update`。H2 profile 已关掉 update，改由 `db/h2` 脚本建表。

表名、唯一键和枚举见 [project_doc/db/PHASE1_SCHEMA.md](../project_doc/db/PHASE1_SCHEMA.md)。
