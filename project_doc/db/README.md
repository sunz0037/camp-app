# 校园OA系统数据库设计文档

首期以仓库根目录 [db/README.md](../../db/README.md)、[`db/migration`](../../db/migration)、[`db/seed`](../../db/seed) 和 [PHASE1_SCHEMA.md](./PHASE1_SCHEMA.md) 为准，接口对照见 [PHASE1.md](./PHASE1.md)。下文是旧版说明，其中的成绩、访客和 `GRADE_ADMIN` / `LOGISTICS` 已不是首期范围，不作为首期执行入口。

## 一、数据库概述

本数据库设计用于校园OA系统，包含以下功能模块：
1. 学生信息管理
2. 学生应急联系人管理
3. 考试成绩管理
4. 校园公告
5. 用户来访登记
6. 数据查询权限管理

## 二、数据库表结构

### 1. 用户表 (users)
存储所有系统用户信息，包括学生、教职工和管理员。

**主要字段：**
- `id`: 用户ID（主键）
- `username`: 用户名（唯一）
- `password`: 密码（加密存储）
- `real_name`: 真实姓名
- `user_type`: 用户类型（1-学生，2-教职工，3-管理员）
- `status`: 状态（0-禁用，1-启用）

### 2. 学生信息表 (students)
存储学生的详细信息，与用户表一对一关联。

**主要字段：**
- `id`: 学生ID（主键）
- `user_id`: 关联用户ID（外键）
- `student_no`: 学号（唯一）
- `grade`: 年级
- `class_name`: 班级
- `major`: 专业
- `department`: 院系
- `status`: 学籍状态（0-休学，1-在读，2-毕业，3-退学）

### 3. 学生应急联系人表 (emergency_contacts)
存储学生的应急联系人信息，一个学生可以有多个联系人。

**主要字段：**
- `id`: 联系人ID（主键）
- `student_id`: 学生ID（外键）
- `contact_name`: 联系人姓名
- `relationship`: 关系（父亲、母亲、监护人等）
- `phone`: 联系电话
- `is_primary`: 是否主要联系人

### 4. 课程表 (courses)
存储课程基本信息。

**主要字段：**
- `id`: 课程ID（主键）
- `course_code`: 课程代码（唯一）
- `course_name`: 课程名称
- `credit`: 学分
- `teacher_id`: 授课教师ID（外键）
- `semester`: 学期
- `academic_year`: 学年

### 5. 考试成绩表 (exam_scores)
存储学生的考试成绩信息。

**主要字段：**
- `id`: 成绩ID（主键）
- `student_id`: 学生ID（外键）
- `course_id`: 课程ID（外键）
- `exam_type`: 考试类型（期中、期末、平时等）
- `score`: 成绩
- `grade_point`: 绩点
- `semester`: 学期
- `academic_year`: 学年

**唯一约束：** 同一学生、同一课程、同一考试类型、同一学期只能有一条记录

### 6. 校园公告表 (announcements)
存储校园公告信息。

**主要字段：**
- `id`: 公告ID（主键）
- `title`: 公告标题
- `content`: 公告内容
- `category`: 公告分类
- `publisher_id`: 发布人ID（外键）
- `publish_time`: 发布时间
- `expire_time`: 过期时间
- `is_top`: 是否置顶
- `is_urgent`: 是否紧急
- `target_audience`: 目标受众

### 7. 用户来访登记表 (visitor_registrations)
存储来访登记信息。

**主要字段：**
- `id`: 登记ID（主键）
- `visitor_name`: 来访人姓名
- `visitor_phone`: 来访人电话
- `visit_purpose`: 来访目的
- `visit_target_user_id`: 访问目标用户ID（外键）
- `visit_date`: 来访日期
- `entry_time`: 实际进入时间
- `exit_time`: 实际离开时间
- `status`: 状态（0-待审核，1-已批准，2-已进入，3-已离开，4-已拒绝）

### 8. 角色表 (roles)
存储系统角色信息。

**主要字段：**
- `id`: 角色ID（主键）
- `role_code`: 角色代码（唯一）
- `role_name`: 角色名称
- `status`: 状态

### 9. 权限表 (permissions)
存储系统权限信息，支持树形结构。

**主要字段：**
- `id`: 权限ID（主键）
- `permission_code`: 权限代码（唯一）
- `permission_name`: 权限名称
- `resource_type`: 资源类型（menu-菜单，button-按钮，data-数据）
- `parent_id`: 父权限ID（支持权限树）

### 10. 角色权限关联表 (role_permissions)
角色和权限的多对多关联表。

### 11. 用户角色关联表 (user_roles)
用户和角色的多对多关联表。

### 12. 教师班级关联表 (teacher_classes)
存储教师与班级的关联关系，用于权限控制。遵循权限最小化原则，教师仅可查询所管理班级的信息。

**主要字段：**
- `id`: 关联ID（主键）
- `teacher_id`: 教师用户ID（外键）
- `grade`: 年级
- `class_name`: 班级名称
- `subject`: 任教科目
- `is_class_teacher`: 是否班主任（0-否，1-是）

**唯一约束：** 同一教师、同一年级、同一班级只能有一条记录

### 13. 年级管理员年级关联表 (grade_admin_grades)
存储年级管理员与年级的关联关系，用于权限控制。遵循权限最小化原则，年级管理员仅可查询所管理年级的信息。

**主要字段：**
- `id`: 关联ID（主键）
- `admin_id`: 年级管理员用户ID（外键）
- `grade`: 年级
- `department`: 所属院系
- `description`: 说明

**唯一约束：** 同一管理员、同一年级只能有一条记录

### 14. 数据查询权限管理表 (data_query_permissions)
细粒度的数据查询权限控制，遵循权限最小化原则，防止用户信息泄露。

**主要字段：**
- `id`: 权限ID（主键）
- `user_id`: 用户ID（可为空，表示角色权限）
- `role_id`: 角色ID（可为空，表示用户权限）
- `data_type`: 数据类型（student-学生信息，score-成绩，emergency_contact-应急联系人，visitor-来访登记等）
- `query_scope`: 查询范围（all-全部，own-仅自己，department-本部门，grade-本年级，class-本班级，custom-自定义）
- `scope_value`: 范围值（当query_scope为custom/grade/class时使用，JSON格式，如{"grade":"2024","class":"计算机1班"}）
- `field_permissions`: 字段权限（JSON格式，控制可查询的字段，空表示全部字段）
- `can_export`: 是否可导出（默认0-否）
- `can_print`: 是否可打印（默认0-否）

**约束：** user_id 和 role_id 至少有一个不为空

**权限最小化原则：**
- 教师角色：仅可查询本班级（class）的相关信息，不可导出和打印
- 年级管理员角色：仅可查询本年级（grade）的相关信息，不可导出和打印
- 学生角色：仅可查询自己的（own）信息
- 后勤人员：可查看和管理来访登记，可导出和打印
- 班级/学生考勤数据：教师仅限本班，年级管理员仅限本年级，系统管理员可全局导出

### 15. 班级每日考勤汇总表 (class_attendance_daily)
按“班级 + 日期”维度汇总每日考勤，用于统计应到/实到/缺勤。

**主要字段：**
- `attendance_date`: 考勤日期
- `grade` / `class_name`: 年级/班级
- `expected_count`: 应到人数
- `present_count`: 实到人数
- `absent_count`: 缺勤人数
- `late_count`: 迟到人数
- `leave_count`: 请假人数
- `recorder_id`: 记录人（教师/班主任）

**唯一约束：** 同一天、同一班级仅一条汇总记录

### 16. 学生每日考勤记录表 (student_attendance_records)
按“学生 + 日期”维度记录出勤异常（仅记录异常，不存储正常出勤），支持家长（紧急联系人）确认。

**主要字段：**
- `student_id`: 学生ID（外键）
- `attendance_date`: 考勤日期
- `grade` / `class_name`: 冗余便于查询
- `status`: 出勤状态（仅记录异常：1-缺勤，2-迟到，3-请假，4-早退）
- `reason_type`: 缺勤/异常原因（sick-生病，competition-比赛，personal-事假，other-其他）
- `reason_detail`: 原因描述
- `notified_contact_id` / `notified_contact_name`: 已通知的紧急联系人
- `notified_method`: 通知方式（phone/sms/wechat）
- `attachment_url`: 证明材料（如病假条）
- `recorder_id`: 记录人（教师/班主任）

**唯一约束：** 同一学生、同一日期仅一条记录

## 三、表关系说明

### 核心关系
1. **users ↔ students**: 一对一关系（一个用户对应一个学生记录）
2. **students ↔ emergency_contacts**: 一对多关系（一个学生可以有多个应急联系人）
3. **students ↔ exam_scores**: 一对多关系（一个学生有多条成绩记录）
4. **courses ↔ exam_scores**: 一对多关系（一门课程有多条成绩记录）
5. **users ↔ courses**: 一对多关系（一个教师可以教授多门课程）
6. **users ↔ announcements**: 一对多关系（一个用户可以发布多条公告）
7. **users ↔ visitor_registrations**: 一对多关系（一个用户可以被多人访问）

### 权限关系
1. **users ↔ user_roles ↔ roles**: 多对多关系（用户可以有多个角色）
2. **roles ↔ role_permissions ↔ permissions**: 多对多关系（角色可以有多个权限）
3. **users/roles ↔ data_query_permissions**: 多对多关系（用户或角色可以有多个数据查询权限）
4. **users ↔ teacher_classes**: 一对多关系（一个教师可以管理多个班级，用于权限控制）
5. **users ↔ grade_admin_grades**: 一对多关系（一个年级管理员可以管理多个年级，用于权限控制）
6. **students ↔ student_attendance_records**: 一对多关系（一个学生可以有多条每日考勤记录）

## 四、索引设计

### 主要索引
- 所有外键字段都建立了索引
- 常用查询字段建立了索引（如学号、用户名、日期等）
- 唯一约束字段自动创建唯一索引

### 查询优化建议
1. 学生信息查询：使用 `student_no`、`grade`、`class_name` 索引
2. 成绩查询：使用 `student_id`、`course_id`、`semester` 组合索引
3. 公告查询：使用 `publish_time`、`status`、`is_top` 索引
4. 来访登记查询：使用 `visit_date`、`status` 索引

## 五、数据完整性

### 外键约束
- 所有外键都设置了适当的删除策略（CASCADE、SET NULL、RESTRICT）
- 学生相关数据使用 CASCADE，确保数据一致性
- 用户相关数据使用 RESTRICT 或 SET NULL，防止误删

### 唯一约束
- 用户名、学号、课程代码等关键字段设置唯一约束
- 成绩表设置组合唯一约束，防止重复录入

### 检查约束
- 数据查询权限表确保 user_id 和 role_id 至少有一个不为空

## 六、初始化数据

### 默认角色
- **ADMIN**: 系统管理员（拥有系统所有权限）
- **TEACHER**: 教师（仅可查询本班级相关信息，遵循权限最小化原则）
- **GRADE_ADMIN**: 年级管理员（仅可查询该年级相关信息，遵循权限最小化原则）
- **STUDENT**: 学生（可查看个人信息和成绩）
- **LOGISTICS**: 后勤人员（可管理来访登记，原安保人员角色）

### 基础权限
系统预置了常用的数据操作权限，包括：
- 学生信息的查看、编辑、删除
- 成绩的查看、编辑
- 公告的查看、发布
- 来访登记的查看、管理
- 权限管理

## 八、使用说明

1. **执行SQL脚本**
   ```bash
   mysql -u username -p database_name < schema.sql
   ```

2. **创建数据库**
   ```sql
   CREATE DATABASE campus_oa CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   USE campus_oa;
   SOURCE schema.sql;
   ```

3. **注意事项**
   - 建议使用 MySQL 5.7+ 或 MariaDB 10.2+
   - 字符集使用 utf8mb4，支持完整的 Unicode 字符
   - 生产环境建议调整 AUTO_INCREMENT 起始值
   - 根据实际需求调整字段长度和索引策略

## 九、扩展建议

1. **日志表**: 可添加操作日志表，记录用户操作历史
2. **文件表**: 可添加文件管理表，统一管理附件
3. **消息表**: 可添加消息通知表，支持系统消息推送
4. **审批流程表**: 可添加审批流程表，支持工作流审批
5. **数据字典表**: 可添加数据字典表，统一管理枚举值

