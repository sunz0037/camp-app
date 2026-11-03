# 数据库脚本

权威目录在仓库根 `db/`。`src/main/resources/db/` 里的脚本是历史全量（含考试、访客），首期不要把它当作建表入口。

## 本地空库

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS camp DEFAULT CHARSET utf8mb4;"
mysql -u root -p camp < db/migration/V1__phase1_schema.sql
mysql -u root -p camp < db/seed/V1__phase1_demo.sql
```

应用配置见 `src/main/resources/application.properties`。本环境没有远程库时，只执行以上两个文件即可。

## 演示账号

| 用户 | id | 开发态身份 |
| --- | --- | --- |
| admin | 1 | `dev-ADMIN` |
| academic | 2 | `dev-ACADEMIC`，授权年级 1 |
| teacher | 3 | `dev-TEACHER`，授权班级 1 |
| student | 4 | `dev-STUDENT`，学籍在初一1班 |

密码登录后置。联调使用开发态请求头，见 README「开发态鉴权」。

## 首期没有的表

`V1` 不建考试成绩、访客登记。这两块路由默认关闭（`camp.phase2.enabled=false`）。
