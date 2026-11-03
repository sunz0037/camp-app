# 数据库设计变更日志

## 首期对齐（phase 1）

脚本在仓库根目录 `db/migration/`、`db/seed/`，执行顺序见 `db/README.md`。表、唯一键和枚举见 [PHASE1_SCHEMA.md](./PHASE1_SCHEMA.md)。

- 账号只保留 `sys_users` + `roles` 底座，不建密码重置或首次激活流程。
- 排课改为 `edu_class_courses` + `edu_class_course_sessions`（`period_start` / `period_end`）。
- 考勤改为 `edu_attendance_summaries`（唯一键 `class_id + attendance_date`）和 `edu_attendance_records`。
- 数据范围改为 `sys_teacher_classes`、`sys_affairs_grades`。
- 公告改为 `oa_announcements`、`oa_announcement_scopes`、`oa_announcement_reads`。
- 新增 `sys_audit_logs`。角色收敛为 `ADMIN` / `ACADEMIC` / `TEACHER` / `STUDENT`。
- 成绩、访客保留在旧脚本中，标记 phase:2，不进入首期必跑迁移。



## 版本 2.0 - 权限最小化原则调整（2024）

### 变更概述
为遵循权限最小化原则，防止用户信息被泄露或售卖，对数据库设计进行了以下调整：

### 1. 角色调整

#### 新增角色
- **GRADE_ADMIN（年级管理员）**：新增年级管理员角色，仅可查询该年级相关信息

#### 角色修改
- **TEACHER（教师）**：权限范围调整为仅可查询本班级相关信息（原为可查看和管理学生信息、成绩等）
- **LOGISTICS（后勤人员）**：原 SECURITY（安保人员）角色更名为后勤人员，功能保持不变

#### 角色列表（更新后）
1. **ADMIN** - 系统管理员（拥有系统所有权限）
2. **TEACHER** - 教师（仅可查询本班级相关信息）
3. **GRADE_ADMIN** - 年级管理员（仅可查询该年级相关信息）
4. **STUDENT** - 学生（可查看个人信息和成绩）
5. **LOGISTICS** - 后勤人员（可管理来访登记）

### 2. 新增数据表

#### 2.1 教师班级关联表 (teacher_classes)
**目的**：实现教师与班级的关联，支持权限最小化控制

**主要字段**：
- `teacher_id`: 教师用户ID
- `grade`: 年级
- `class_name`: 班级名称
- `subject`: 任教科目
- `is_class_teacher`: 是否班主任

**权限控制**：通过此表关联，系统可自动限制教师仅能查询所管理班级的学生信息、成绩、应急联系人等数据。

#### 2.2 年级管理员年级关联表 (grade_admin_grades)
**目的**：实现年级管理员与年级的关联，支持权限最小化控制

**主要字段**：
- `admin_id`: 年级管理员用户ID
- `grade`: 年级
- `department`: 所属院系

**权限控制**：通过此表关联，系统可自动限制年级管理员仅能查询所管理年级的学生信息、成绩、应急联系人等数据。

### 3. 数据查询权限表调整

#### 3.1 query_scope 字段扩展
**原值**：`all`, `own`, `department`, `class`, `custom`

**新增值**：`grade`（本年级）

**更新后**：`all`, `own`, `department`, `grade`, `class`, `custom`

#### 3.2 scope_value 字段说明更新
- 支持 `grade` 和 `class` 查询范围时，可通过 JSON 格式存储具体范围值
- 示例：`{"grade":"2024","class":"计算机1班"}`

#### 3.3 字段权限说明
- `field_permissions` 字段支持 JSON 格式，控制可查询的字段列表
- 示例：`["student_no","name","grade","class_name"]`
- 空值表示可查询全部字段

### 4. 权限配置初始化

#### 4.1 教师角色权限配置
- **学生信息查询**：`query_scope = 'class'`，不可导出和打印
- **成绩查询**：`query_scope = 'class'`，不可导出和打印
- **应急联系人查询**：`query_scope = 'class'`，不可导出和打印

#### 4.2 年级管理员角色权限配置
- **学生信息查询**：`query_scope = 'grade'`，不可导出和打印
- **成绩查询**：`query_scope = 'grade'`，不可导出和打印
- **应急联系人查询**：`query_scope = 'grade'`，不可导出和打印

#### 4.3 学生角色权限配置
- **学生信息查询**：`query_scope = 'own'`，不可导出和打印
- **成绩查询**：`query_scope = 'own'`，不可导出和打印

#### 4.4 后勤人员角色权限配置
- **来访登记查询**：`query_scope = 'all'`，可导出和打印（业务需要）

### 5. 权限最小化原则实施

#### 5.1 数据访问控制
- **教师**：仅能访问 `teacher_classes` 表中关联的班级数据
- **年级管理员**：仅能访问 `grade_admin_grades` 表中关联的年级数据
- **学生**：仅能访问自己的数据

#### 5.2 数据导出控制
- 教师和年级管理员角色默认禁止导出和打印数据（`can_export = 0`, `can_print = 0`）
- 防止批量数据泄露和售卖

#### 5.3 字段级权限控制
- 通过 `field_permissions` 字段可进一步限制可查询的字段
- 敏感字段（如身份证号、家庭地址等）可通过此机制进行保护

### 6. 数据库表结构变更总结

#### 新增表（2张）
1. `teacher_classes` - 教师班级关联表
2. `grade_admin_grades` - 年级管理员年级关联表

#### 修改表（2张）
1. `roles` - 角色表（新增 GRADE_ADMIN 角色，修改 SECURITY 为 LOGISTICS）
2. `data_query_permissions` - 数据查询权限管理表（扩展 query_scope 支持 grade）

#### 未变更表
- 其他业务表结构保持不变，仅通过权限控制实现数据访问限制

### 7. 迁移建议

#### 7.1 数据迁移
1. 执行新的 SQL 脚本创建新表
2. 将现有的教师与班级关联关系迁移到 `teacher_classes` 表
3. 为年级管理员用户创建 `grade_admin_grades` 关联记录
4. 更新 `data_query_permissions` 表中的权限配置

#### 7.2 应用层调整
1. 查询学生信息时，根据用户角色和关联表进行数据过滤
2. 教师查询时，JOIN `teacher_classes` 表，仅返回关联班级的数据
3. 年级管理员查询时，JOIN `grade_admin_grades` 表，仅返回关联年级的数据
4. 禁止教师和年级管理员角色的导出和打印功能

#### 7.3 权限验证
1. 在数据查询接口中增加权限验证逻辑
2. 验证用户是否有权限访问请求的数据范围
3. 验证用户是否有权限导出/打印数据

### 8. 安全建议

1. **定期审计**：定期检查 `teacher_classes` 和 `grade_admin_grades` 表的关联关系，确保权限配置正确
2. **操作日志**：记录所有数据查询和导出操作，便于追踪和审计
3. **敏感字段保护**：对身份证号、家庭地址等敏感字段，建议通过 `field_permissions` 进行额外限制
4. **权限审批**：教师和年级管理员的权限分配应经过审批流程

### 9. 回滚方案

如需回滚到之前的版本：
1. 删除新增的表：`teacher_classes`、`grade_admin_grades`
2. 恢复 `roles` 表中的角色定义
3. 调整 `data_query_permissions` 表中的权限配置
4. 更新应用层代码，移除权限过滤逻辑

---

**变更日期**：2024年
**变更原因**：遵循权限最小化原则，防止用户信息泄露和售卖
**影响范围**：权限管理模块、数据查询模块


## 版本 2.1 - 班级/学生每日考勤（2024）

### 变更概述
- 新增班级每日考勤汇总与学生每日考勤记录，支持按班级维度统计应到/实到，并按学生维度记录缺勤原因，方便与家长（紧急联系人）确认。

### 新增表
1. `class_attendance_daily`：班级每日考勤汇总（考勤日期 + 年级 + 班级 唯一）
2. `student_attendance_records`：学生每日考勤明细（考勤日期 + 学生唯一），包含状态与缺勤原因、家长通知信息

### 关键字段/逻辑
- `class_attendance_daily`: expected_count/present_count/absent_count/late_count/leave_count，统计当日48/50等数据
- `student_attendance_records`: status（正常/缺勤/迟到/请假/早退）、reason_type（sick/competition/personal/other）、reason_detail、notified_contact_id/notified_method/notified_at 支持家校沟通
- 外键与索引：student_id、attendance_date、grade/class 便于按班级/日期查询；唯一键避免重复打卡

### 优化
- 为降低数据库压力，`student_attendance_records` 仅记录异常出勤（缺勤/迟到/请假/早退），正常出勤不入库；增加 CHECK(status IN (1,2,3,4)) 约束

### 权限建议
- 教师：仅可记录/查看本班考勤；年级管理员：仅可查看本年级；系统管理员：全局查看与导出

**影响范围**：考勤管理、家校沟通
