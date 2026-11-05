# 校园 OA 二期接口补记（N1–N3）

> 相对首期契约 `api-contract-v1-phase1.md`。前缀、响应包、四档角色、开发态 `Bearer dev-ROLE` 不变。家长仍同 `STUDENT`。
>
> 本轮做了写路径加固、文件上传、考试成绩，以及来访、主题和改密别名。表用已合入的 V3（#4），登录加固用已合入的 Wave A（#5）。不另建 `exam_schedules` / `sys_files` / `sys_user_settings`。

## 完成

### N1 写路径

创建和更新成功时，`data` 返回保存后的对象（含 `id`），前端可直接刷新列表。校验失败时 HTTP 仍是 200，看包里的 `code`：`400` 缺参、`403` 越权、`404` 不存在、`409` 冲突、`422` 业务规则。

| 模块 | 行为 |
| --- | --- |
| 学生 | 学号、班级、学籍状态 0–3、性别 0/1。无 `userId` 时按学号建 `STUDENT` 账号。软删。班级必须在数据范围内。状态变更写 `STUDENT_STATUS_CHANGE`，其它档案变更写 `STUDENT_PROFILE_CHANGE`，明细不写手机号和身份证 |
| 用户 | `POST` 才接受 `password`（至少 6 位，缺省 `123456`）。`PUT` **忽略** `password`。启停 `PUT /users/{id}/status?status=0\|1`。教务只能管授权年级里的学生、任课教师，以及尚未绑定班级的教师/学生；不能创建或修改 `ADMIN`/`ACADEMIC`。被学籍或排课引用时删除返回 409，应改停用 |
| 课程库 | `courseCode`、`courseName`、`credit` 必填。代码冲突 409。已被排课引用时删除 409 |
| 排课 | `academicYear` + `semester`（1 或 2）必填。节次 `weekday` 1–7，`period` 1–20，班内同年学期重叠 409。写操作校验班级数据范围。`POST /class-courses/batch` 增加查询参数 `academicYear`、`semester` |
| 考勤 | 汇总仍是同一事务写异常明细。补录 `POST /attendances/records`（别名 `/students`）校验学生在范围内，同一学生同一天同一课程重复 409。`DELETE /attendances/records/{id}` 与 `/students/{id}` 等价 |
| 公告 | 返回带 `scopeType`/`scopeIds` 的对象。教师只能发到本人班级，且不能置顶。删除会先清受众和已读，避免外键失败。附件若是 `/api/v1/files/{id}` 会核对文件存在 |

审计事件码在原 1–6 上增加（MySQL 跑 `V4__phase2_audit_events.sql`，H2 建库即包含）。改密仍走 Wave A，事件码 2：

| code | event_type |
| --- | --- |
| 7 | USER_ACCOUNT |
| 8 | COURSE_WRITE |
| 9 | CLASS_COURSE_WRITE |
| 10 | ATTENDANCE_WRITE |
| 11 | ANNOUNCEMENT_WRITE |

### N2 文件

见 `FILE_STORAGE.md` 与 `phase2-files.md`。

`GET /api/v1/files` 列目录，`POST /api/v1/files/upload` 上传，`GET /api/v1/files/{fileId}` 下载。`purpose` 增加 `STUDENT_IMPORT`、`STUDENT_EXPORT`。公告附件和头像仍用 `/api/v1/files/{id}`；学生导入导出可以先落到这份文件再读回。

### N3 考试成绩

`camp.phase2.exam.enabled=true`（默认开）。写权限复用 `course:write`（管理员、教务）。表是 `edu_exams`、`edu_exam_scores`。`/exams` 与 `/exam-schedules`、`/exam-scores` 与 `/scores` 是同一套服务，不双写。

| 前端路径 | 旧路径 | 说明 |
| --- | --- | --- |
| `GET/POST /exams`，`GET/PUT/DELETE /exams/{id}` | `/exam-schedules` | 同一张 `edu_exams`。`PUT /exams/{id}/cancel` 与旧取消一样返回 422 |
| `GET/POST /exam-scores`，`GET/PUT/DELETE /exam-scores/{id}` | `/scores` | 同一张 `edu_exam_scores`。`examId` 即 `examScheduleId` |

`/exams` 接受前端字段：`name`→考试名，`startTime`→考试日期，`subject`→按课程名唯一匹配 `courseId`，`className`（可加 `gradeName`）→按班级名唯一匹配 `classId`。没传学年学期时，按考试日期补：9 月到次年 1 月为第一学期，2 月到 8 月为第二学期。旧路径不补这些缺省，缺了仍是 400。

创建考试在旧路径上必填 `examName`、`examDate`、`classId`、`courseId`、`academicYear`、`semester`（1 或 2）。同一班级、课程、日期、名称冲突返回 409。已有成绩时删除返回 409。

录入成绩必填 `examScheduleId`（即 `edu_exams.id`，别名 `examId`）、`studentId`、`score`（0–100）。学生只能看本人。同一场考试同一学生重复返回 409。响应里的 `gradePoint` 按 90/80/70/60 换成 4/3/2/1/0，`gradeLevel` 换成 A/B/C/D/E，**都不入库，也不是 GPA 汇总**。请求里的 `gradeLevel` 忽略。

列表会带上课程名、班级名、年级名、学生姓名学号，方便页面展示。关键字可匹配标题、课程名、班级名；成绩还可匹配学生姓名和学号。`/exams` 可用 `grade`（年级名称）、`classId`、`courseId` 收窄。`examType` 筛选仍是 400。`isPublic=true` 或 `status=CANCELLED` 得到空列表，因为这些列不存在，现有行都视为不公开、未取消。

### N4 来访、主题、改密（来自 #6，接到 #4 的表）

| 能力 | 路径 | 表 |
| --- | --- | --- |
| 来访 | `GET/POST /visitors`，`PUT/DELETE /visitors/{id}`，`POST .../approve|reject|check-in|check-out` | `oa_visitors`，流水 `oa_visitor_logs` |
| 主题 | `GET/PUT /me/settings`，`themeKey` 为 `default` / `dark` / `campus-green` / `campus-blue` | `sys_user_preferences` |
| 改密 | `POST /auth/change-password` | 不新写密码逻辑，直接走 Wave A 的 `PUT /auth/password`（复杂度、历史、锁定、审计都在 #5） |

来访：学生 403。教师可登记，并查看本人登记或本人被访的单。审批、入校、离校只给管理员和教务。新建固定 `PENDING`。列表上的电话和证件号脱敏。旧 `visitor_registrations` 控制器仍由 `camp.phase2.visitor.enabled=false` 关闭，避免和本路径抢映射。没有公开预约。

创建时 `reason`、`visitPurpose` 写入 `purpose`，`idNo` 写入证件号。被访人仍要 `hostUserId`；只传 `hostName` 返回 400「被访人需要 hostUserId，暂不按姓名解析」，不按姓名查用户。

## 明确不做（PHASE2-BLOCKED）

| 项 | 原因 |
| --- | --- |
| 公开预约、门岗独立端点、`LOGISTICS` | 来访只给已登录的四档角色，不新增角色 |
| 短信验证码、公开找回 | `camp.sms.enabled=false`。密码策略已由 #5 接管 |
| 分享的增删改、目录分享级联、对象存储以外的实现 | 列表、建目录、按已有 `oa_file_shares` 读取已在 `phase2-files.md`。不写分享，也不做 S3 |
| 考试类型、公开标记、取消状态、结束时间、备注、`grade_point` 列 | `edu_exams` 没有这些列。写入时忽略（取消标记除外，返回 422）。`PUT /exams/{id}/cancel` 与 `PUT /exam-schedules/{id}/cancel` 都是 422 |
| 按被访人姓名解析 `hostUserId` | 来访创建只认 id。`hostName` 不能代替 `hostUserId` |
| 独立 `/todos`、GPA、`/statistics/**` | 未做 |
| 教师录入本班成绩 | 写入口是 `course:write`，教师没有该权限 |

## 联调提示

```bash
./gradlew bootRun --args='--spring.profiles.active=h2'
```

`Authorization: Bearer dev-ADMIN`（或 `dev-ACADEMIC` / `dev-TEACHER` / `dev-STUDENT`）。写完用列表接口刷新即可。
