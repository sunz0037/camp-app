# 校园OA系统

校园OA系统（学生信息管理、学生应急联系人管理、考试成绩管理、校园公告、用户来访登记、数据查询权限管理）

## 项目文档

所有项目文档已统一整理到 `project_doc/` 目录下，请查看：

- **[项目文档目录](./project_doc/README.md)** - 文档导航和分类说明

### 快速链接

- **API文档**: [project_doc/api/](./project_doc/api/)
- **数据库文档**: [project_doc/db/](./project_doc/db/)
- **使用指南**: [project_doc/guides/](./project_doc/guides/)

## 技术栈

- **框架**: Spring Boot 3.2.0
- **数据库**: MySQL 8.0
- **ORM**: Spring Data JPA / Hibernate
- **密码加密**: BCrypt
- **构建工具**: Gradle

## 快速开始

### 1. 数据库初始化

参考 [数据库初始化文档](./project_doc/guides/DATABASE_INIT.md)

### 2. 配置数据库连接

参考 [数据库配置文档](./project_doc/guides/DATABASE_CONFIG.md)

### 3. 启动应用

```bash
./gradlew bootRun
```

### 4. 登录系统

参考 [登录系统使用指南](./project_doc/guides/LOGIN_GUIDE.md)

## 首期范围

接口契约：[project_doc/api/api-contract-v1-phase1.md](project_doc/api/api-contract-v1-phase1.md)。前缀 `/api/v1`。

角色四档：`ADMIN` > `ACADEMIC` > `TEACHER` > `STUDENT`（家长与学生同权，没有独立家长角色）。

首期打通：组织（年级/班级）、学生与应急联系人、课程库与班级授课（`weekday` + `periodStart` + `periodEnd` + `roomNo`）、考勤汇总与异常明细、公告与已读、工作台 `GET /api/v1/dashboard/overview`。

考试/成绩、访客、短信找回默认关闭（`camp.phase2.enabled=false`、`camp.sms.enabled=false`）。与旧文档里的六角色（含后勤/访客/年级管理员）和考试访客全量接口不是同一交付面。

建表：[db/README.md](db/README.md)。

## 本地联调

- 端口 `8080`，没有额外 context-path，接口前缀 `/api/v1`
- 开发库用 H2：`jdbc:h2:file:./data/camp`（也可改成 `jdbc:h2:mem:camp`）
- 启动时 Flyway 执行仓根 `db/migration`（`V1` 空库，`V2` 旧库对齐），随后执行 `db/seed/phase1_demo.sql`
- 表结构以这两份 SQL 为准，`spring.jpa.hibernate.ddl-auto=none`，Hibernate 不改表
- CORS 允许 `http://localhost:5173` 与 `http://localhost:4173`（同时保留 3000）
- 演示账号密码都是 `Demo@123456`。空库种子用户依次是 `admin`、`academic01`、`teacher01`、`student01`，可用 `X-Dev-User-Id` 绑定（空库上通常是 1–4）

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

## 开发态鉴权

只在 `--spring.profiles.active=dev` 或 `camp.dev-auth.enabled=true` 时生效。业务接口按这个身份做权限和数据范围，不走完整密码登录。

```http
Authorization: Bearer dev-TEACHER
X-Dev-User-Id: 3
X-Dev-Class-Ids: 1
X-Dev-Grade-Ids: 1
```

`X-Dev-Role: TEACHER` 与 Bearer 二选一。不传用户或范围头时，默认用户 1 管理员、2 教务、3 教师、4 学生，范围回退到 `sys_affairs_grades` / `sys_teacher_classes` 或学籍。

## 主要功能

- 四档角色与开发态联调身份
- 学生信息与应急联系人
- 年级、班级、课程与结构化课表
- 考勤汇总（班级+日期）与异常明细
- 公告发布范围与已读
- 工作台概览

## 项目结构

```
camp-app/
├── src/
│   ├── main/
│   │   ├── java/com/camp/app/
│   │   │   ├── api/          # API响应模型
│   │   │   ├── config/       # 配置类
│   │   │   ├── controller/   # REST控制器
│   │   │   ├── entity/       # JPA实体
│   │   │   ├── model/        # DTO/VO模型
│   │   │   ├── repository/   # 数据访问层
│   │   │   ├── service/      # 服务层
│   │   │   └── util/         # 工具类
│   │   └── resources/
│   │       ├── db/           # 数据库脚本
│   │       └── application.properties
│   └── test/
├── project_doc/              # 项目文档（统一目录）
└── build.gradle
```

## 开发说明

详细开发文档请查看 [project_doc/](./project_doc/) 目录。

