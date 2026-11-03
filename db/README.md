# 数据库脚本

权威脚本与数据库 PR（`db/migration` 的 V1 / V2，以及 `db/seed/phase1_demo.sql`）一致。`src/main/resources/db/` 里的脚本是历史全量，不是首期入口。

| 文件 | 用途 |
| --- | --- |
| `db/migration/V1__phase1_schema.sql` | 空库建首期表 |
| `db/migration/V2__phase1_legacy_align.sql` | 已有旧库时改名、补列 |
| `db/seed/phase1_demo.sql` | 演示数据。密码都是 `Demo@123456` |

## MySQL

空库：

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS camp DEFAULT CHARSET utf8mb4;"
mysql -u root -p camp < db/migration/V1__phase1_schema.sql
mysql -u root -p camp < db/seed/phase1_demo.sql
```

旧库只跑 V2，不要再跑 V1：

```bash
mysql -u root -p camp < db/migration/V2__phase1_legacy_align.sql
mysql -u root -p camp < db/seed/phase1_demo.sql
```

## H2 联调

`application-dev.properties` 使用 `jdbc:h2:file:./data/camp`（可改成 `jdbc:h2:mem:camp`）。`./gradlew bootRun --args='--spring.profiles.active=dev'` 读取仓根 `db/migration/V1__phase1_schema.sql` 和 `db/seed/phase1_demo.sql`。空 H2 不跑 V2（V2 只给已有 MySQL 旧库）。H2 不支持 MySQL 的 `PREPARE` 和部分索引名，启动时只在内存里改写语句，不改 SQL 文件。

Hibernate `ddl-auto=none`，不根据实体建表。

## 演示账号

种子按用户名写入，不写死自增 ID。空库上顺序一般是：

| 用户名 | 开发态 | 常见 id |
| --- | --- | --- |
| admin | `dev-ADMIN` | 1 |
| academic01 | `dev-ACADEMIC` | 2 |
| teacher01 | `dev-TEACHER` | 3 |
| student01 | `dev-STUDENT` | 4 |

id 若不是这组，用请求头 `X-Dev-User-Id` 指到种子用户。密码 `Demo@123456`。

首期不建考试成绩、访客表。
