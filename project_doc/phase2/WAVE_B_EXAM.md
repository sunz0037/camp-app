# Wave B — 考试与成绩（草案）

本文件只定 DDL 和接口清单。本轮不实现控制器，不把表放进 H2 启动脚本，也不把 `camp.phase2.enabled` 设为 true。

现有 `ExamScheduleController`（`/api/v1/exam-schedules`）和 `ScoreController`（`/api/v1/scores`）在该开关打开后才会加载，且没有首期 Data Scope。实现 Wave B 时以本文为准，不要直接打开旧控制器。建议成绩路径用 `/api/v1/exam-scores`，避免和旧的 `/api/v1/scores` 并存。

## 表

#4 已把考试成绩落成 `edu_exams` / `edu_exam_scores`（`db/migration/V3__phase2_schema.sql`，H2 在 `db/h2/schema.sql`）。说明见 `project_doc/db/PHASE2_SCHEMA.md`。原先起草的 `exam_schedules` / `exam_scores` 脚本与这套表重复，已删除，不要再执行，也不要并进 H2 启动脚本。

实现接口时以 `edu_exams`（`course_id`、`class_id`、`academic_year`、`semester`）和 `edu_exam_scores`（`exam_id`、`student_id`、`score`）为准。旧实体 `ExamSchedule` / `ExamScore` 仍指向 `exam_schedules` / `exam_scores`，不要直接打开旧控制器。

## 数据范围

与首期相同，服务端过滤：

| 角色 | 安排 | 成绩 |
| --- | --- | --- |
| ADMIN | 全校 | 全校 |
| ACADEMIC | `grade_id` 在 `sys_affairs_grades` 内；`class_id` 为空时看年级，否则看班级所属年级 | 同上，经学生的 `grade_id` |
| TEACHER | 班主任班级，或 `sys_teacher_classes` / 自己任课的 `edu_class_courses` | 这些班级里的学生 |
| STUDENT | 本人 `students.class_id` / `grade_id` 能对上的安排 | 仅 `students.user_id` 等于自己 |

越权返回 403。学生不能录入或修改成绩。

## 接口清单（未实现）

统一分页：`page` 从 1，`pageSize` 默认 20、最大 100。列表 `data: { items, total, page, pageSize }`。

### 考试安排 `/api/v1/exam-schedules`

| 方法 | 路径 | 谁可以写 | 说明 |
| --- | --- | --- | --- |
| GET | `/exam-schedules` | 登录且在范围内只读 | 过滤 `gradeId`、`classId`、`courseId`、`academicYear`、`semester`、`examType`、`status` |
| GET | `/exam-schedules/{id}` | 范围内只读 | 范围外 403，不存在 404 |
| POST | `/exam-schedules` | ADMIN、ACADEMIC | 必填考试名、时间、类型、学年学期；年级/班级/课程至少一个维度 |
| PUT | `/exam-schedules/{id}` | ADMIN、ACADEMIC | 已有成绩时改课程或班级返回 409 |
| POST | `/exam-schedules/{id}/cancel` | ADMIN、ACADEMIC | `status=0`，不物理删除 |

请求体示例：

```json
{
  "examName": "高一期末",
  "examType": "期末",
  "examDate": "2026-01-16T09:00:00",
  "academicYear": "2025-2026",
  "semester": 1,
  "gradeId": 1,
  "classId": 1,
  "courseId": 1,
  "roomNo": "A101",
  "isPublic": false,
  "targetGrade": "高一"
}
```

### 成绩 `/api/v1/exam-scores`

| 方法 | 路径 | 谁可以写 | 说明 |
| --- | --- | --- | --- |
| GET | `/exam-scores` | 范围内只读 | 学生忽略传入的 `studentId`，只查本人。过滤 `examScheduleId`、`courseId`、`semester`、`academicYear`、`examType` |
| GET | `/exam-scores/{id}` | 范围内只读 | 学生只能看自己的 |
| POST | `/exam-scores` | ADMIN、ACADEMIC、TEACHER | 教师只能录自己范围内的学生。同一安排同一学生 409 |
| PUT | `/exam-scores/{id}` | 同上 | 改分数；缺考把 `score` 空着，原因写 `remarkDetail` |
| DELETE | `/exam-scores/{id}` | ADMIN、ACADEMIC | 教师不开放物理删除 |

请求体示例：

```json
{
  "examScheduleId": 1,
  "studentId": 1,
  "courseId": 1,
  "examType": "期末",
  "score": 92.5,
  "semester": "1",
  "academicYear": "2025-2026",
  "remarkDetail": null
}
```

## 实现时注意

- 保持 `ddl-auto=none`。表已经在 #4 的 V3 与 `db/h2/schema.sql` 里，不要再加一份考试建表脚本。
- 成绩录入写审计时沿用现有事件码，不要新造 `event_code`。若需要单独的成绩审计码，先改 `chk_sys_audit_event` 的约束，那是另一张迁移。
- 不回写首期考勤、学籍、排课表。
