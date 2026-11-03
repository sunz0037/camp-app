# 校园 OA 二期接口补记（N1–N3）

> 相对首期契约 `api-contract-v1-phase1.md`。前缀、响应包、四档角色、开发态 `Bearer dev-ROLE` 不变。家长仍同 `STUDENT`。
>
> 本轮做了写路径加固、文件、考试成绩脚手架。访客、短信、主题、独立待办不做。

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

审计事件码在原 1–6 上增加（MySQL 跑 V3，H2 建库即包含）：

| code | event_type |
| --- | --- |
| 7 | USER_ACCOUNT |
| 8 | COURSE_WRITE |
| 9 | CLASS_COURSE_WRITE |
| 10 | ATTENDANCE_WRITE |
| 11 | ANNOUNCEMENT_WRITE |

### N2 文件

见 `FILE_STORAGE.md`。

`POST /api/v1/files/upload`，`multipart` 字段 `file`，可选 `purpose=AVATAR|ANNOUNCEMENT|OTHER`。

```json
{ "fileId": 1, "url": "/api/v1/files/1", "originalName": "a.png", "contentType": "image/png", "sizeBytes": 3, "purpose": "AVATAR" }
```

### N3 考试成绩（脚手架）

`camp.phase2.exam.enabled=true`（默认开）。写权限复用已有 `course:write`（管理员、教务），不新增角色。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET/POST | `/exam-schedules` | 类型：月考/期中/期末，也接受 MONTHLY/MIDTERM/FINAL。创建人用登录用户，兼容旧头 `X-User-Id` |
| PUT/DELETE | `/exam-schedules/{id}` | 更新、删除 |
| PUT | `/exam-schedules/{id}/cancel` | 状态改为取消 |
| GET/POST/PUT/DELETE | `/scores` `/scores/{id}` | 学生仅本人。分数 0–100。未传绩点时按 90/80/70/60 写成 4/3/2/1/0，**不是 GPA 汇总** |

表：`exam_schedules`、`exam_scores`。旧库若已有同名表且列不一致，V3 的 `CREATE TABLE IF NOT EXISTS` 不会改列，需要人工对齐。

## 明确不做（PHASE2-BLOCKED）

| 项 | 原因 |
| --- | --- |
| `/visitors/**` 预约、门岗核销、公开端点 | 未实现安全边界。控制器仅在 `camp.phase2.visitor.enabled=true` 时加载，默认关闭。不要把公开预约直接暴露 |
| 短信验证码、公开找回、主题切换 | P2，`camp.sms.enabled=false` |
| 独立 `/todos` | P3 |
| 成绩 GPA / 统计报表 `/statistics/**` | 只记单科 `gradePoint` |
| 教师录入本班成绩 | 当前写入口是 `course:write`，教师没有该权限 |
| 第五角色 `LOGISTICS` | 不新增。访客若以后要独立边界，再单独立项 |

## 联调提示

```bash
./gradlew bootRun --args='--spring.profiles.active=h2'
```

`Authorization: Bearer dev-ADMIN`（或 `dev-ACADEMIC` / `dev-TEACHER` / `dev-STUDENT`）。写完用列表接口刷新即可。
