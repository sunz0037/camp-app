# 校园OA 首期接口契约骨架（Phase 1）

> 本文件为后端联调契约（架构评审 v1.0.1）。首期 DDL 见仓库根 `db/migration/`、`db/seed/`、`db/README.md`。
>
> **开发态鉴权**（仅 `spring.profiles.active=dev` 或 `camp.dev-auth.enabled=true`）：
> `Authorization: Bearer dev-ADMIN|dev-ACADEMIC|dev-TEACHER|dev-STUDENT`，或请求头 `X-Dev-Role` 取同样的角色名。
> 可选 `X-Dev-User-Id`、`X-Dev-Class-Ids`、`X-Dev-Grade-Ids`（逗号分隔）。业务接口按该身份做权限和数据范围。完整密码登录后置。


- 版本：v1.0.1
- 前缀：`/api/v1`
- 统一响应：`{ "code": 200, "message": "success", "data": {}, "timestamp": "ISO-8601" }`
- 鉴权：`Authorization: Bearer <accessToken>`；AccessToken 2h + RefreshToken 7d
- 角色（四档）：`ADMIN` > `ACADEMIC` > `TEACHER` > `STUDENT`（家长同 `STUDENT` 同权）
- 非目标（Phase 2，本文仅占位不实现）：考试/成绩、访客/后勤、统计报表、短信验证码、独立待办

---

## 0. 约定

| 项 | 约定 |
| --- | --- |
| 分页 | `page`（从1）、`pageSize`（默认20，最大100）；列表 `data: { items, total, page, pageSize }` |
| 错误码 | `400` 参数 / `401` 未登录 / `403` 无权限或越权数据范围 / `404` 不存在 / `409` 冲突 / `422` 业务校验 |
| 数据范围 | 服务端强制过滤；禁止仅靠前端藏按钮。`ADMIN` 全校；`ACADEMIC` 授权年级；`TEACHER` 授权班级/所授课程；`STUDENT` 仅本人 |
| 脱敏 | 列表/导出中身份证、手机号按角色掩码；详情在权限内可见明文 |
| 审计 | 登录失败、密码重置、学生导入导出、学籍关键变更写审计日志 |
| 排课 | 禁止纯字符串 `schedule_info`；使用结构化节次（见课程模块） |

---

## 1. 认证与个人中心 `/api/v1/auth`

| 方法 | 路径 | 角色 | 说明 |
| --- | --- | --- | --- |
| POST | `/auth/login` | 公开 | 统一入口（不分学生/教职工双页也可前端分 Tab）。返回 `accessToken, refreshToken, isFirstLogin, userInfo{id,realName,roles,permissions,dataScope}` |
| POST | `/auth/refresh` | 持 refresh | 无感续期 |
| POST | `/auth/logout` | 登录用户 | 吊销 refresh（可选黑名单） |
| GET | `/auth/me` | 登录用户 | 当前用户 + 权限码 + 数据范围摘要 |
| PUT | `/auth/profile` | 登录用户 | 改本人可编辑档案（头像、邮箱等；学号/角色不可自改） |
| PUT | `/auth/password` | 登录用户 | 已知旧密码改密 |
| POST | `/auth/activate` | 登录且首次 | 首次登录强制改密（可绑手机）；`isFirstLogin=true` 时其它写接口可拒绝 |
| POST | `/auth/admin-reset-password` | ADMIN/ACADEMIC | **首期替代短信找回**：重置指定用户密码为临时口令并强制下次改密 |

**不做（Phase1）**：`/auth/sms-code`、公开短信找回、人脸登录。

---

## 2. 工作台 `/api/v1/dashboard`

| 方法 | 路径 | 角色 | 说明 |
| --- | --- | --- | --- |
| GET | `/dashboard/overview` | 登录用户 | 按角色聚合：指标卡、今日课表（教师/学生）、最新公告（含未读）、快捷入口元数据 |

**不做**：独立 `/todos` CRUD（首页待办若需要，Phase1 可用公告未读 + 考勤异常数聚合，不单开表接口）。

---

## 3. 组织：年级 / 班级 `/api/v1/grades` `/api/v1/classes`

| 方法 | 路径 | 角色与范围 |
| --- | --- | --- |
| GET/POST/PUT/DELETE | `/grades` `/grades/{id}` | ADMIN 全量写；ACADEMIC 授权年级读写；TEACHER/STUDENT 不开放管理（下拉字典用精简 list） |
| GET/POST/PUT/DELETE | `/classes` `/classes/{id}` | 同上；班级必含 `gradeId`；`headTeacherId` 可选 |
| GET | `/grades/options` `/classes/options` | 登录用户；按 data scope 返回下拉项，消灭手填班级名 |

删除策略：有下属班级/学生时 `409`，禁止硬删或仅软删由实现选定并在 OpenAPI 注明。

---

## 4. 学生与应急联系人 `/api/v1/students`

| 方法 | 路径 | 角色与范围 |
| --- | --- | --- |
| GET | `/students` | ADMIN 全校；ACADEMIC 本年级；TEACHER 本班只读列表 |
| GET | `/students/{id}` | 同上；STUDENT 仅 `id` 对应当前绑定学籍 |
| POST/PUT | `/students` `/students/{id}` | ADMIN/ACADEMIC；字段含学号、班级、学籍状态等 |
| DELETE | `/students/{id}` | ADMIN（或 ACADEMIC 按验收）；建议软删 `isDeleted` |
| GET | `/students/import-template` | ADMIN/ACADEMIC |
| POST | `/students/batch-import` | ADMIN/ACADEMIC；事务或行级错误回执 |
| GET | `/students/export` | ADMIN/ACADEMIC/TEACHER（本班）；导出脱敏 |
| GET/POST | `/students/{studentId}/emergency-contacts` | 管理端按学生范围；**STUDENT 仅本人 studentId** |
| PUT/DELETE | `/emergency-contacts/{id}` | 同上；`PUT .../primary` 设主要联系人 |

学生账号：创建学籍时可可选创建 `STUDENT` 用户；家长若需登录，首期发 **同角色 STUDENT 账号**（与学生同权），不建 parent 绑定表。

---

## 5. 课程库与班级授课 `/api/v1/courses` `/api/v1/class-courses`

### 5.1 课程库

| 方法 | 路径 | 角色 |
| --- | --- | --- |
| CRUD | `/courses` | ADMIN/ACADEMIC 写；TEACHER 只读所授相关；字典 options 全登录可用 |

### 5.2 班级授课（排课）

实体关键字段：

```json
{
  "academicYear": "2025-2026",
  "semester": 1,
  "classId": 1,
  "courseId": 1,
  "teacherId": 1,
  "sessions": [
    { "weekday": 1, "periodStart": 1, "periodEnd": 2, "roomNo": "A101" }
  ]
}
```

| 方法 | 路径 | 角色与范围 |
| --- | --- | --- |
| GET/POST/PUT/DELETE | `/class-courses` | ADMIN/ACADEMIC 写；TEACHER 只读所授；STUDENT 见「我的课表」 |
| GET | `/class-courses/my-timetable` | TEACHER/STUDENT | 当前用户课表（按 sessions 展开） |

**禁止**用自由文本 `scheduleInfo` 作为唯一课表来源。

---

## 6. 考勤 `/api/v1/attendances`

模型：`summaries`（班级×日）+ `records`（异常学生明细）。

异常枚举：`LATE | EARLY_LEAVE | PERSONAL_LEAVE | SICK_LEAVE | ABSENT`  
明细状态：`PENDING | CONFIRMED | CLEARED`（首期可默认 CONFIRMED）

| 方法 | 路径 | 角色与范围 |
| --- | --- | --- |
| GET | `/attendances/summaries` | ADMIN/ACADEMIC/TEACHER 按范围 |
| POST | `/attendances/summaries` | TEACHER（本班）/ACADEMIC/ADMIN；**同一事务**写入勾选的异常 `records`；幂等键建议 `classId+date` |
| PUT/DELETE | `/attendances/summaries/{id}` | 同写权限；改汇总时同步明细 |
| GET | `/attendances/records` | 管理端按范围；**STUDENT：仅本人** |
| POST/PUT | `/attendances/records` | 补录/销假；教师本班 |

可选：`courseId` 挂在明细上（某节课异常）；汇总仍按「班级+日」。首期**不做**学生扫码/定位自签。

---

## 7. 公告 `/api/v1/announcements`

| 方法 | 路径 | 角色 |
| --- | --- | --- |
| GET | `/announcements` | 登录用户；按发布范围过滤；返回 `isRead` |
| GET | `/announcements/unread-count` | 登录用户 |
| GET | `/announcements/{id}` | 可见范围内；打开即记已读 |
| POST | `/announcements` | ADMIN/ACADEMIC 全校或指定组织；**TEACHER 仅本班/所授范围** |
| PUT | `/announcements/{id}` | 作者或 ADMIN/ACADEMIC |
| PUT | `/announcements/{id}/top` | ADMIN/ACADEMIC |
| PUT | `/announcements/{id}/withdraw` | 作者或 ADMIN/ACADEMIC |
| DELETE | `/announcements/{id}` | ADMIN 或作者策略按验收 |

发布范围建议：`scopeType: ALL | GRADE | CLASS` + `scopeIds[]`。  
已读表：`(announcement_id, user_id)` 唯一。

---

## 8. 用户与权限 `/api/v1/users` `/api/v1/roles` `/api/v1/data-scopes`

| 方法 | 路径 | 角色 |
| --- | --- | --- |
| CRUD + 启停 | `/users` | ADMIN；ACADEMIC 可管本范围教职工/学生账号（若验收允许） |
| GET/PUT | `/users/{id}/roles` | ADMIN |
| GET | `/roles` `/permissions/tree` | ADMIN |
| PUT | `/roles/{id}/permissions` | ADMIN |
| GET/PUT | `/teachers/{userId}/classes` | ADMIN/ACADEMIC | 教师-班级授权（替代裸 JSON scope） |
| GET/PUT | `/academics/{userId}/grades` | ADMIN | 教务-年级授权（表 `sys_affairs_grades`） |

**不做**：文档中的 `LOGISTICS`/`VISITOR`/`GRADE_ADMIN` 角色码；`scope_json` 万能配置首期不用。

---

## 9. 文件 `/api/v1/files`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/files/upload` | 登录用户；返回 `fileId, url`；用于头像/公告附件 |
| DELETE | `/files/{fileId}` | 上传者或 ADMIN |

---

## 10. Phase 2 仅清单（不实现）

- `/exam-schedules`、`/scores`、GPA 统计
- `/visitors/**` 含公开预约与门岗核销
- `/statistics/**` 报表中心
- 短信验证码与公开找回密码
- 独立 `/todos`

---

## 11. 与文档 V2.0 的差异摘要

| 文档项 | 首期处理 |
| --- | --- |
| 六角色含后勤/访客/年级管理员 | 映射为四档；教务覆盖年级统筹 |
| 考试/成绩/访客/报表 API | 二期 |
| `schedule_info` 字符串 | 改为 `sessions[]` |
| 裸 `scope_json` | 改为教师-班级、教务-年级关联表 |
| 短信找回 | 改为管理员/教务重置 |
| 96 接口全量 | 本骨架约 50 个以内可交付面 |

---

## 12. 下一步

1. @数据库大师 按本契约对齐表字段与枚举命名（建议 API 用字符串枚举，库内可用 tinyint 映射）。
2. @架构师 确认分层（网关鉴权 → 应用服务 → 数据范围拦截）。
3. @web程序员 按路径生成 API client / mock。
4. 定稿后补 OpenAPI 3 文件（`openapi-phase1.yaml`）。


---

## 附录 A. 按钮权限码（与前端对齐）

`org:write` · `student:write` · `student:import` · `student:export` · `course:write` · `class_course:write` · `attendance:record` · `announcement:publish` · `announcement:top` · `announcement:withdraw` · `user:write` · `role:write`
