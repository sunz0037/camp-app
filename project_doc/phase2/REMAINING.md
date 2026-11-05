# 二期剩余排期 — 库/API 后置项

> **本轮不实现。** 本文件只登记后续 Cursor 队列。不改 H2、不改 Flyway、不改已合入的 Java API，也不改 `application.properties` 里的开关默认值。

对照基线是 main 上已合入的四件事，下一轮不要重做：

| 已合入 | 内容 | 不要再做 |
| --- | --- | --- |
| #4 | Flyway `V3__phase2_schema.sql`、`V4__phase2_audit_events.sql`，以及 H2 里对应的表 | 另写考试 / 访客 / 网盘 / 偏好建表脚本，或改已有列的含义 |
| #5 | 密码与登录加固（JWT、改密、锁定、审计） | 用短信替换 `PUT /api/v1/auth/password`、`POST /api/v1/auth/admin-reset-password` |
| #7 | 写路径、文件上传、考试成绩、来访、`GET/PUT /api/v1/me/settings` | 再开一套 `/exams`、`/exam-scores` 或第二套主题表 |
| #9 | 网盘列表、建目录、下载，以及按已有 `oa_file_shares` 读取。契约见 `project_doc/api/phase2-files.md` | 再改这些路径，或给分享加增删改。没有新 migration |

契约里已经写成「明确不做」的，仍以 `project_doc/api/api-contract-v1-phase2.md` 的 PHASE2-BLOCKED 为准。本文件只把还没落库/API、或只差前端消费的四项排成队列。

## 本轮不实现

下面 R1–R4 **本轮都不写代码**。不新增迁移，不改 `db/h2/schema.sql`，不打开 `camp.sms.enabled`，不增加 `/statistics/**`，不改 `/me/settings` 与 `/grades`、`/classes` 的路径和字段。

## 队列

| ID | 项 | 表 | API | 依赖 FE | 下一句任务 |
| --- | --- | --- | --- | --- | --- |
| R1 | 短信通知 | 无 | 无可用通道 | 通道本身否；公开找回页才是 | 保持 `camp.sms.enabled=false` 和 #5 改密语义不变，另开任务接入短信网关并落发送记录，替换内存日志。 |
| R2 | 报表聚合 | 无 | 无（首页概览不算） | 接口否；报表页是 | 不建汇总表，在现有业务表上加只读 `/api/v1/statistics/**`，沿用 Data Scope，且不要改 `dashboard/overview`。 |
| R3 | 主题 UI 消费 | 有 `sys_user_preferences` | 有 `GET/PUT /api/v1/me/settings` | 是 | 后端无必做项；前端读取 `themeKey` 做换肤，本仓库不改该 API。 |
| R4 | 年级班级拆页 | 有 `grades`、`classes` | 有 `/api/v1/grades`、`/api/v1/classes` | 是 | 纯拆页不加表、不加接口；前端分成两个路由分别调现有列表。缺字段时再单开后端任务。 |

建议顺序：R3、R4 只动前端，可并行；R2 是只读接口，不挡它们；R1 要供应商和可能的新表，放在最后，且默认开关保持关闭。

### R1 短信通知

**现状**

- 无短信表，无发送记录表，无供应商配置表。验证码在 `VerificationCodeServiceImpl` 的进程内 `Map` 里，注释写明生产环境应改存储；`generateAndSendCode` 只打日志，不调用短信网关。
- `PasswordResetController`（`POST /api/v1/auth/send-verification-code`、`POST /api/v1/auth/reset-password`）标了 `@ConditionalOnProperty(name = "camp.sms.enabled", havingValue = "true")`。`application.properties` 里 `camp.sms.enabled=false`，默认不加载这条控制器。
- 契约写明不做短信验证码和公开找回。已登录改密、管理员重置走 #5，不经过短信。
- 旧文档（`FEATURES_STATUS.md`、`API_STATUS.md`）把「发送验证码」标成已实现。那是内存打日志，不是短信通道。不要按那些勾选状态开工。

**下一 Cursor 任务**

保持 `camp.sms.enabled=false` 和 #5 改密语义不变，另开任务接入一家短信网关并落发送记录，替换 `VerificationCodeServiceImpl` 的内存日志。

**依赖 FE：** 通道与落表否。只有产品要做公开找回页时才依赖前端；该页面不在本项必做范围内。

### R2 报表聚合

**现状**

- 无报表表、无统计汇总表、无物化视图。
- 无 `/api/v1/statistics/**`。`api-contract-v1-phase2.md` 写明 GPA 与 `/statistics/**` 未做。
- 已有 `GET /api/v1/dashboard/overview`：当前角色、数据范围内学生数、未读公告数、今日课节、今日课表、最新公告。这是首页概览，不是按年级、班级、课程或日期聚合的报表。下一轮不要改这个接口来塞报表。
- `edu_exam_scores` 只存 `score`。响应里的 `gradePoint` 按单行分数换算，不入库，也不是均分或及格率。
- 旧设计稿 `GET /api/statistics/students|scores|visitors`（`API_DESIGN.md`）没有实现，前缀也不是现行的 `/api/v1`。不要按旧路径补控制器。

**下一 Cursor 任务**

不新建汇总表，在现有 `students`、`edu_exam_scores`、`oa_visitors` 上增加只读 `/api/v1/statistics/**`（年级班级人数、均分与及格率、来访按状态/日期），沿用首期 Data Scope，且不要改 `GET /api/v1/dashboard/overview`。

**依赖 FE：** 接口否。报表页面是。

### R3 主题 UI 消费

**现状**

- 有表。`sys_user_preferences` 在 `db/migration/V3__phase2_schema.sql` 和 `db/h2/schema.sql`：`theme_key`（默认 `default`）、`locale`、`extras`。禁止把色值 hex 写入该表。演示种子里 `student01` 为 `campus-blue`。
- 有 API。`GET/PUT /api/v1/me/settings` 只读写 `themeKey`，允许 `default`、`dark`、`campus-green`、`campus-blue`。`locale` 和 `extras` 列在，当前接口不读不写。
- 换肤 UI 归前端。本轮前端隐藏，不消费 `themeKey`。

**下一 Cursor 任务**

后端无必做项；在 camp-web 登录后用 `GET /api/v1/me/settings` 的 `themeKey` 映射 Token，设置页 `PUT` 同一接口。本仓库不改该 API，也不要为换肤新增表。

**依赖 FE：** 是。若以后要暴露 `locale` 或 `extras`，再单开后端任务扩展同一接口，不要新表。

### R4 年级班级拆页

**评估：纯拆页不需要新表，也不需要新接口。**

**现状**

- 有表，而且本来就是两张。`grades`（代码、名称、学段、学段内序号、状态）和 `classes`（`grade_id` 外键、班号、容量、班主任）在首期 `V1__phase1_schema.sql` / `db/h2/schema.sql`。数据范围用 `sys_teacher_classes`、`sys_affairs_grades`，不是页面模型。
- 有 API，而且已经分开：
  - `GET/POST/PUT/DELETE /api/v1/grades`，以及 `/options`、`/all-enabled`、`/by-type/{gradeType}`、`/{id}`
  - `GET/POST/PUT/DELETE /api/v1/classes`，以及 `/options?gradeId`、`/by-grade/{gradeId}`、`/by-grade/{gradeId}/enabled`、`/{id}`
  - 教务可管年级：`/api/v1/academics/{userId}/grades`；教师班级：`/api/v1/teachers/{teacherId}/classes`
- `PHASE2_BACKEND_PLAN.md` 把「年级/班级拆页 IA」列为非目标，首期合并页保留。缺的是前端两个路由，不是库或接口。

只有拆页要展示上述列里没有的内容（例如独立入学届、班级相册、和 `grade_id` 不同的树）时，才需要新列或新接口。那一步先列出字段，再开后端任务。不要预先建空表。

**下一 Cursor 任务**

前端把年级、班级分成两个路由，分别调用已有 `/api/v1/grades` 与 `/api/v1/classes?gradeId=`；本仓库不加表、不加接口。

**依赖 FE：** 是。

## 硬约束（执行 R1–R4 时仍有效）

1. 不改 `db/migration/V1`–`V4` 与 `db/h2/schema.sql` 里已有表、列、默认值的含义。新表只用更后的迁移版本，并同步 H2，且与本文件无关的表不动。
2. 不改已合路径的请求和响应：`/auth/password`、`/files`（含 #9 的列表、上传、下载、`/files/folders`）、`/exam-schedules`、`/scores`、`/visitors`、`/me/settings`、`/grades`、`/classes`、`/dashboard/overview`。分享增删改仍不做。
3. `spring.jpa.hibernate.ddl-auto=none`。表只来自 SQL。
4. 家长继续同 `STUDENT`。不借本队列新增后勤角色或公开预约。
