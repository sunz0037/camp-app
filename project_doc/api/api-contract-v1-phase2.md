# 校园 OA 二期接口契约（Phase 2）

> 在首期契约之上追加。首期路径、分页和错误体不变。表前缀继续使用 `edu_` / `oa_` / `sys_`，不改首期表。
>
> DDL：`db/migration/V3__phase2_exam_visitor_settings.sql`（MySQL），`db/h2/schema.sql` 同名表（H2）。  
> 种子：`db/seed/phase2_demo.sql`，H2 启动时由 `db/h2/seed.sql` 末尾一并执行。密码仍是 `Demo@123456`。

- 版本：v1.1.0
- 前缀：`/api/v1`
- 统一响应：`{ "code": 200, "message": "success", "data": {}, "timestamp": "ISO-8601" }`
- 鉴权：与首期相同。开发态 `Authorization: Bearer dev-ADMIN|dev-ACADEMIC|dev-TEACHER|dev-STUDENT`，可选 `X-Dev-User-Id`、`X-Dev-Class-Ids`、`X-Dev-Grade-Ids`
- 角色：`ADMIN` > `ACADEMIC` > `TEACHER` > `STUDENT`（家长同 STUDENT）

旧控制器 `/exam-schedules`、`/scores` 只有 `camp.phase2.enabled=true` 才注册，**不是**本契约。`/visitors` 以本文件为准；`camp.legacy-visitor-api.enabled` 默认 false。

---

## 0. 约定

| 项 | 约定 |
| --- | --- |
| 分页 | `page` 从 1，`pageSize` 默认 20、最大 100。`data: { items, total, page, pageSize }` |
| 错误码 | `400` 参数，`401` 未登录，`403` 无权限或越权，`404` 不存在，`409` 冲突，`422` 业务校验。HTTP 状态与首期一样由 body `code` 表达 |
| 时间 | `yyyy-MM-ddTHH:mm:ss` |
| 审计 | 改密、找回密码写入 `sys_audit_logs`（`event_code=2`）。明细不写密码、身份证、手机号明文 |
| 脱敏 | 来访列表的证件号、电话脱敏。详情对管理员、教务，以及登记人或被访人返回明文 |

---

## 1. 考试安排 `/api/v1/exams`

表 `edu_exams`。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | number | |
| examCode | string | 可省略，服务端生成。唯一 |
| examName | string | 必填 |
| gradeId / gradeName | number / string | 年级可选。只传班级时按班级回填年级 |
| classId / className | number / string | 班级可选 |
| courseId / subjectName | number / string | 科目。传 `courseId` 且未传科目名时用课程名 |
| examType | string | `MIDTERM` `FINAL` `MONTHLY` `QUIZ` |
| startTime / endTime | datetime | 结束必须晚于开始 |
| isPublic | boolean | 学生只能看见公开安排 |
| status | string | `SCHEDULED` `FINISHED` `CANCELED`。缺省 `SCHEDULED` |
| roomNo / remark | string | |
| createdBy / createdAt / updatedAt | | |

筛选项：`gradeId`、`classId`、`examType`、`isPublic`（`true`/`false`/`1`/`0`）、`status`、`keyword`（名称、科目、编码、考场）。

| 方法 | 路径 | 角色 |
| --- | --- | --- |
| GET | `/exams` `/exams/{id}` | 登录用户，按数据范围过滤 |
| POST | `/exams` | ADMIN 全校；ACADEMIC 授权年级；TEACHER 仅本班（必须带 `classId`）；STUDENT 403 |
| PUT | `/exams/{id}` | 同上。未传字段保持原值 |
| DELETE | `/exams/{id}` | 同上。已有成绩时 `409` |

数据范围：

- ADMIN：全部
- ACADEMIC：本校级（未填年级和班级）以及授权年级/班级
- TEACHER：本班考试、所在年级且未指定班级的考试、公开的校级考试
- STUDENT：公开，且校级或本人年级/班级

---

## 2. 成绩 `/api/v1/exam-scores`

表 `edu_exam_scores`。同一考试同一学生唯一，重复录入 `409`。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | number | |
| examId / examName | | 必填 |
| studentId / studentNo / studentName | | 必填学生 |
| classId / className / gradeId | | 由学籍带出 |
| score | number | 0 到 150，超出 `422` |
| gradeLevel | string | 可空。未传时按分数生成 A/B/C/D/E（90/80/70/60） |
| recordedBy / recordedByName / recordedAt | | 录入人取当前用户 |
| remark | string | |

筛选项：`examId`、`studentId`、`classId`、`keyword`（考试名、学号、姓名、备注）。

| 方法 | 路径 | 角色 |
| --- | --- | --- |
| GET | `/exam-scores` `/exam-scores/{id}` | ADMIN 全部；ACADEMIC 授权年级且考试可见；TEACHER 本班学生且考试可见；STUDENT 只读本人，含未公开考试的本人成绩 |
| POST PUT DELETE | `/exam-scores` `/exam-scores/{id}` | ADMIN / ACADEMIC / TEACHER 按上表范围。STUDENT 403 |

STUDENT 若显式传他人 `studentId`，返回 `403`。

---

## 3. 来访 `/api/v1/visitors`

表 `oa_visitors`。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id / visitNo | | 单号可省略，服务端生成 |
| visitorName | string | 必填 |
| idCard | string | 证件，可空 |
| phone | string | 必填 |
| purpose | string | 事由，必填 |
| hostUserId / hostName | | 被访人。新建时至少一项；传用户 ID 时补姓名 |
| appointmentTime | datetime | 新建必填 |
| arrivalTime / leaveTime | datetime | 入校、离校动作写入 |
| status | string | `PENDING` `APPROVED` `REJECTED` `CHECKED_IN` `CHECKED_OUT` |
| remark / createdBy / createdAt / updatedAt | | |

新建忽略传入状态，固定 `PENDING`。若传入其它状态，`400`。

| 方法 | 路径 | 角色 |
| --- | --- | --- |
| GET | `/visitors` `/visitors/{id}` | ADMIN、ACADEMIC 全部；TEACHER 本人登记或本人被访；STUDENT 403 |
| POST | `/visitors` | ADMIN、ACADEMIC、TEACHER |
| PUT | `/visitors/{id}` | ADMIN、ACADEMIC 可改字段；TEACHER 只能改本人待审单。状态不在这里改 |
| DELETE | `/visitors/{id}` | ADMIN、ACADEMIC 可删；TEACHER 只能删本人待审单 |
| POST | `/visitors/{id}/approve` | ADMIN、ACADEMIC。仅 `PENDING` → `APPROVED` |
| POST | `/visitors/{id}/reject` | ADMIN、ACADEMIC。仅 `PENDING` → `REJECTED` |
| POST | `/visitors/{id}/check-in` | ADMIN、ACADEMIC。仅 `APPROVED` → `CHECKED_IN`，写入 `arrivalTime` |
| POST | `/visitors/{id}/check-out` | ADMIN、ACADEMIC。仅 `CHECKED_IN` → `CHECKED_OUT`，写入 `leaveTime` |

动作 body 可空：`{ "remark": "可选" }`。非法流转 `422`。TEACHER 调动作接口 `403`。

筛选项：`status`、`hostUserId`、`keyword`（姓名、被访人、事由、单号）。

---

## 4. 密码

复用 `sys_users.password`，BCrypt，不明文落库。种子口令仍是 `Demo@123456`。

### 4.1 修改密码

`POST /api/v1/auth/change-password`

```json
{ "oldPassword": "Demo@123456", "newPassword": "Demo@654321" }
```

登录用户。旧密码错误 `400`。新密码少于 6 位 `400`。成功后原 `PUT /api/v1/auth/password` 仍可用，两条路径都写审计。

### 4.2 找回密码（最小）

`POST /api/v1/auth/reset-password` 公开，无需登录。`camp.sms.enabled=true` 时仍走原短信控制器，本实现不注册。

教工（`identityType=2`）：登录名、姓名、手机号，验证码固定 `000000`。

```json
{
  "identityType": 2,
  "identifier": "teacher01",
  "realName": "班主任李想",
  "phone": "13800000003",
  "verificationCode": "000000",
  "newPassword": "Demo@123456"
}
```

学生（`identityType=1`）六要素，对种子数据：

| 要素 | 种子值 |
| --- | --- |
| 学号 identifier | `S20260001` |
| 姓名 realName | `学生王小明` |
| 身份证 idCard | `110101201001011234`（虚构） |
| 联系人 contactName | `王芳` |
| 电话 contactPhone | `13900000001` |
| 关系 relationship | `母亲` |

成功 `data.success=true`。失败 `400`，不回显密码。

---

## 5. 用户主题 `/api/v1/me/settings`

表 `sys_user_settings`。没有记录时读作 `default`（v1.3.4 蓝紫壳，由前端解释，本接口只存键）。

| 方法 | 说明 |
| --- | --- |
| GET | `{ "themeKey": "default" }` |
| PUT | body `{ "themeKey": "default" \| "dark" \| "campus-green" }`，其它值 `400` |

只读写当前登录用户。

---

## 6. 种子一览

| 编码 | 内容 |
| --- | --- |
| EXAM-MID-MATH | 初一1班期中数学，公开，`SCHEDULED` |
| EXAM-FINAL-MATH | 初一年级期末数学，公开，不指定班级 |
| EXAM-QUIZ-MATH | 初一1班测验，不公开，`FINISHED`。学生看不见安排，能看见本人成绩 |
| 成绩 | 学号 `S20260001` 三场：92.5 A、78 C、88 B |
| VIS-001 | 张敏，待审，被访教师 |
| VIS-002 | 刘强，已批准，被访教务 |
| VIS-003 | 陈静，已入校，被访管理员 |
| 主题 | admin `default`，student01 `campus-green` |

---

## 7. 角色矩阵

| 能力 | ADMIN | ACADEMIC | TEACHER | STUDENT |
| --- | --- | --- | --- | --- |
| 读考试安排 | 全部 | 授权年级 + 校级 | 本班/所在年级 | 公开且本人范围 |
| 写考试安排 | 全部 | 授权年级 | 本班 | 无 |
| 读成绩 | 全部 | 授权年级 | 本班 | 本人 |
| 写成绩 | 全部 | 授权年级 | 本班 | 无 |
| 读来访 | 全部 | 全部 | 本人相关 | 无 |
| 登记来访 | 是 | 是 | 是 | 无 |
| 审批/入校/离校 | 是 | 是 | 无 | 无 |
| 改密 / 主题 | 本人 | 本人 | 本人 | 本人 |
