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

不要把 MySQL 的 `db/migration` 直接丢给 H2（有 `PREPARE`、库级不唯一的索引名）。适配脚本是 `db/h2/schema.sql` 和 `db/h2/seed.sql`，表名与 MySQL 首期库相同。

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

`dev` 会带上 profile `h2`：内存库 `jdbc:h2:mem:camp`，启动时执行上述两份脚本，`ddl-auto=none`。等价配置见 `src/main/resources/application-h2.properties`。

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
