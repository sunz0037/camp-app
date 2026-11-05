# 校园 OA 二期 / 剩余项 — 后台执行计划（camp-app）

> 基线：首期联调已过（学生/课程课表/考勤/公告 + H2 + `/api/v1` + 四档 RBAC）。  
> 仓库：https://github.com/sunz0037/camp-app  
> 起点：`cursor/phase1-api-alignment-3865` @ `e2ea876`（含 #3 API；DDL 与 #2 同源）。  
> 合入顺序仍：**先 [#2](https://github.com/sunz0037/camp-app/pull/2) 再 [#3](https://github.com/sunz0037/camp-app/pull/3)**（人工合并，本计划不自动 merge）。

## 0. 首期收尾（执行前核对）

| ID | 项 | 说明 | 验收 |
| --- | --- | --- | --- |
| P1-A | 合入 #2→#3 | DDL/H2 再 API；main 与联调分支对齐 | main 可 `h2` 启动冒烟 |
| P1-B | 契约归档 | `project_doc/api` 与实现一致；保留 `dev-*` 鉴权 | 文档与 `/api/v1` 对照无缺口 |

## 1. 二期模块优先级（后台）

### Wave A — 密码与账号加固（优先）

| ID | 交付 | API/库要点 | 验收 |
| --- | --- | --- | --- |
| A1 | 正式 JWT/会话 | 非 h2/dev 走 JWT；保留 h2/dev 的 `Bearer dev-*` 与 `X-Dev-*` | 登录拿 token；过期/刷新；改密后旧 access 失效 |
| A2 | 改密 / 首次改密策略 | 复杂度可配置；历史密码条数默认 0；审计 `PASSWORD_RESET` | 规则可配置；失败码 422，文案即规则 |
| A3 | 登录失败锁定 | `LOGIN_FAIL` 写入 `sys_audit_logs`；连续失败阈值锁定，到期或管理员解锁 | 锁定码 423；解锁后可再登录 |

### Wave B — 考试与成绩

| ID | 交付 | API/库要点 | 验收 |
| --- | --- | --- | --- |
| B1 | 考试安排 CRUD | 表以已合入的 `edu_exams` 为准（#4，`V3__phase2_schema.sql`） | Data Scope 与首期一致 |
| B2 | 成绩录入/查询 | 表以已合入的 `edu_exam_scores` 为准；教师录、学生查本人 | RBAC：ADMIN/ACADEMIC/TEACHER/STUDENT |
| B3 | H2+MySQL 迁移与 seed | 已在 #4 落地：`db/migration` 权威 + `db/h2` 转译 | `bootRun h2` 可演示 |

考试、访客、网盘的表已由 #4 合入（`edu_exams` / `edu_exam_scores`、`oa_visitors`、`oa_files`）。与之重复的 `exam_schedules` / `exam_scores` 草案脚本已删除，不要再执行。接口清单仍见 `WAVE_B_EXAM.md`，本轮不实现这些接口。

### Wave C — 访客

| ID | 交付 | API/库要点 | 验收 |
| --- | --- | --- | --- |
| C1 | 访客登记 CRUD | 表以已合入的 `oa_visitors` / `oa_visitor_logs` 为准（#4） | 列表/审批状态机清晰 |
| C2 | 权限 | ADMIN/ACADEMIC（或后勤角色若二期启用） | 越权 403 |

### Wave D — 文件网盘

| ID | 交付 | API/库要点 | 验收 |
| --- | --- | --- | --- |
| D1 | 文件元数据 + 上传下载 | 表以已合入的 `oa_files` / `oa_file_shares` 为准（#4）；本地盘或可插拔存储 | 鉴权后上下载 |
| D2 | 与业务挂钩（可选） | 公告附件、学生导入导出文件 | 首期导入路径可复用 |

### Wave E — 主题（以后端为准的偏好，可选）

| ID | 交付 | 说明 |
| --- | --- | --- |
| E1 | 用户偏好 API | 表以已合入的 `sys_user_preferences.theme_key` 为准（#4）；**换肤 UI 归前端** |

## 2. 非目标（本轮不做）

- 浅/深主题 UI、Token 视觉（camp-web / Framer）
- 年级/班级拆页 IA（首期合并页保留）
- 家长独立角色（继续同 STUDENT）
- 自动合并 PR #2 / #3

## 3. 执行约定

1. 只改 **camp-app**。正式 DDL 以已合入的 `db/migration/V3__phase2_schema.sql` 与 `db/h2/schema.sql` 为准。不要再另写一套考试/访客建表脚本。
2. 路径前缀继续 `/api/v1`；统一 `BaseResponse`；Data Scope 不破坏首期。
3. 每 Wave 单独可测：H2 冒烟 + 契约片段写入 `project_doc/phase2/`。
4. 开 PR，标题标明 Wave；不要回退或改写首期表（`sys_*` / `edu_*` / `oa_*`，以及首期正在使用的 `grades` / `classes` / `students` / `courses`）。
5. `spring.jpa.hibernate.ddl-auto=none`。表结构只来自 SQL。

## 4. 本轮落地（Wave A）

已在后续 PR 实现，细节与冒烟步骤见 `project_doc/phase2/WAVE_A_AUTH.md`。

- 非 `h2`/`dev` profile：只接受 JWT。`Bearer dev-*` 与 `X-Dev-*` 视为无效令牌。
- `profile=h2`、`profile=dev` 或 `camp.dev-auth.enabled=true`：保留开发态身份，同时仍可走密码登录拿 JWT。
- `PUT /api/v1/auth/password` 按 `camp.auth.password.*` 校验，成功后轮换 access/refresh，并写 `PASSWORD_RESET`。
- 登录失败写 `LOGIN_FAIL`（event_code=1）。连续失败达到 `camp.auth.lock.max-failures` 返回 423，`POST /api/v1/auth/admin-unlock` 可提前解开。
- 锁定计数、刷新令牌吊销、改密后的 `ver` 作废都在进程内，不改首期表。重启后锁定与刷新令牌清空；改密前签发且 `ver=0` 的 access 在重启后会重新有效，直到自然过期。
- 管理员重置的临时口令以 `FORCE$` 前缀加 bcrypt 落库，下次登录 `isFirstLogin=true`，必须再改密。不新增列。

未做：Wave B 的接口与 seed、Wave C–E、把锁定计数和历史密码落表。

上面这句是 Wave A 合入时的记录。考试、来访和下面的网盘已在后续 PR 落地。

## 5. 本轮落地（Wave D）

契约见 `project_doc/api/phase2-files.md`。没有新 migration，表仍是 `V3__phase2_schema.sql` 的 `oa_files` / `oa_file_shares`。

- `GET /api/v1/files` 列表，`POST /api/v1/files/upload` 上传，`GET /api/v1/files/{fileId}` 下载，`POST /api/v1/files/folders` 建目录。
- 字节走 `BlobStore`，默认本地目录 `camp.upload.dir`。
- 公告附件、头像继续用 `/api/v1/files/{id}`。学生导入 `POST /students/import-file`，导出落盘 `GET /students/export-file`。
- 已有分享可以读。不提供分享的增删改。
