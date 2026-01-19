# 校园OA系统API接口清单（摘要版）

## 接口统计
- **总接口数**：95个
- **模块数**：18个
- **已实现**：58个（61%）
- **部分实现**：12个（13%）
- **未实现**：25个（26%）

**最后更新**：2024年12月

> 📋 **实现状态详情**：请查看 `API_STATUS.md` 文档

---

## 一、用户认证与授权（7个接口）✅ 已实现（5个核心接口）

**用途**：用户登录认证、Token管理、密码修改、密码重置等安全相关功能

| 接口路径 | 方法 | 功能说明 | 状态 |
|---------|------|---------|------|
| `/api/auth/login` | POST | 用户登录（支持学生、教工） | ✅ |
| `/api/auth/activate` | POST | 激活用户（首次登录设置密码） | ✅ |
| `/api/auth/logout` | POST | 用户登出 | 🟡 |
| `/api/auth/refresh` | POST | 刷新Token | ❌ |
| `/api/auth/me` | GET | 获取当前用户信息 | ❌ |
| `/api/auth/password` | PUT | 修改密码 | ✅ |
| `/api/auth/send-verification-code` | POST | 发送验证码（密码重置） | ✅ |
| `/api/auth/reset-password` | POST | 重置密码 | ✅ |

**实现说明**：
- ✅ 登录功能已完整实现，支持学生（学号）和教工（工号）登录
- ✅ 首次登录激活功能已实现
- ✅ 密码修改和重置功能已实现
- 🟡 登出功能已实现框架，Token黑名单机制待完善
- ❌ Token刷新和获取当前用户信息需要JWT Token支持

---

## 二、用户管理（6个接口）❌ 未实现

**用途**：系统用户（学生、教职工、管理员）的创建、查询、更新、删除等管理功能

| 接口路径 | 方法 | 功能说明 | 状态 |
|---------|------|---------|------|
| `/api/users` | POST | 创建用户 | ❌ |
| `/api/users` | GET | 查询用户列表 | ❌ |
| `/api/users/{id}` | GET | 查询用户详情 | ❌ |
| `/api/users/{id}` | PUT | 更新用户信息 | ❌ |
| `/api/users/{id}` | DELETE | 删除用户 | ❌ |
| `/api/users/{id}/status` | PUT | 启用/禁用用户 | ❌ |

---

## 三、学生信息管理（7个接口）✅ 已实现

**用途**：学生基本信息的CRUD操作，支持权限过滤、分页查询、批量导入导出

| 接口路径 | 方法 | 功能说明 | 状态 |
|---------|------|---------|------|
| `/api/students` | POST | 创建学生信息 | ✅ |
| `/api/students` | GET | 查询学生列表（权限过滤、分页、关键词搜索） | ✅ |
| `/api/students/{id}` | GET | 查询学生详情 | ✅ |
| `/api/students/{id}` | PUT | 更新学生信息 | ✅ |
| `/api/students/{id}` | DELETE | 删除学生信息 | ✅ |
| `/api/students/batch-import` | POST | 批量导入学生（Excel） | 🟡 |
| `/api/students/export` | GET | 导出学生信息（Excel） | 🟡 |

**权限说明**：
- 系统管理员：可查询全部
- 年级管理员：仅可查询本年级
- 教师：仅可查询本班级
- 学生：仅可查询自己

---

## 四、学生应急联系人管理（6个接口）❌ 未实现

**用途**：管理学生的应急联系人信息，用于考勤异常时通知家长

| 接口路径 | 方法 | 功能说明 | 状态 |
|---------|------|---------|------|
| `/api/students/{studentId}/emergency-contacts` | POST | 添加应急联系人 | ❌ |
| `/api/students/{studentId}/emergency-contacts` | GET | 查询应急联系人列表 | ❌ |
| `/api/emergency-contacts/{id}` | GET | 查询应急联系人详情 | ❌ |
| `/api/emergency-contacts/{id}` | PUT | 更新应急联系人 | ❌ |
| `/api/emergency-contacts/{id}` | DELETE | 删除应急联系人 | ❌ |
| `/api/emergency-contacts/{id}/primary` | PUT | 设置主要联系人 | ❌ |

**权限说明**：同学生信息管理

---

## 五、考试成绩管理（8个接口）✅ 已实现（5个核心接口）

**用途**：学生考试成绩的录入、查询、更新、统计等功能

| 接口路径 | 方法 | 功能说明 | 状态 |
|---------|------|---------|------|
| `/api/scores` | POST | 创建成绩记录 | ✅ |
| `/api/scores/batch` | POST | 批量录入成绩 | 🟡 |
| `/api/scores` | GET | 查询成绩列表（多条件过滤、分页） | ✅ |
| `/api/scores/{id}` | GET | 查询成绩详情 | ✅ |
| `/api/scores/{id}` | PUT | 更新成绩 | ✅ |
| `/api/scores/{id}` | DELETE | 删除成绩 | ✅ |
| `/api/students/{studentId}/scores/statistics` | GET | 查询学生成绩统计（平均分、GPA） | ❌ |
| `/api/scores/export` | GET | 导出成绩（Excel） | 🟡 |

**权限说明**：
- 系统管理员：可查询全部
- 年级管理员：仅可查询本年级
- 教师：仅可查询本班级，可录入本班级成绩
- 学生：仅可查询自己的成绩

---

## 六、课程管理（5个接口）✅ 已实现

**用途**：课程信息的创建、查询、更新、删除等管理功能

| 接口路径 | 方法 | 功能说明 | 状态 |
|---------|------|---------|------|
| `/api/courses` | POST | 创建课程 | ✅ |
| `/api/courses` | GET | 查询课程列表（多条件过滤、分页） | ✅ |
| `/api/courses/{id}` | GET | 查询课程详情 | ✅ |
| `/api/courses/{id}` | PUT | 更新课程信息 | ✅ |
| `/api/courses/{id}` | DELETE | 删除课程 | ✅ |

---

## 六-1、班级授课科目关联管理（8个接口）✅ 已实现

**用途**：管理班级与授课科目的关联关系

| 接口路径 | 方法 | 功能说明 | 状态 |
|---------|------|---------|------|
| `/api/class-courses` | POST | 添加班级授课科目 | ✅ |
| `/api/class-courses/batch` | POST | 批量添加班级授课科目 | ✅ |
| `/api/class-courses/by-class/{classId}` | GET | 查询班级的所有授课科目 | ✅ |
| `/api/class-courses/by-class/{classId}/enabled` | GET | 查询班级的启用授课科目 | ✅ |
| `/api/class-courses/{id}` | GET | 查询详情 | ✅ |
| `/api/class-courses/{id}` | PUT | 更新班级授课科目 | ✅ |
| `/api/class-courses/{id}` | DELETE | 删除班级授课科目 | ✅ |
| `/api/class-courses/batch` | DELETE | 批量删除班级授课科目 | ✅ |

---

## 六-2、考试安排管理（6个接口）✅ 已实现

**用途**：考试安排的创建、查询、更新、删除、取消等管理功能

| 接口路径 | 方法 | 功能说明 | 状态 |
|---------|------|---------|------|
| `/api/exam-schedules` | POST | 创建考试安排 | ✅ |
| `/api/exam-schedules` | GET | 查询考试安排列表（多条件过滤、分页） | ✅ |
| `/api/exam-schedules/{id}` | GET | 查询考试安排详情 | ✅ |
| `/api/exam-schedules/{id}` | PUT | 更新考试安排 | ✅ |
| `/api/exam-schedules/{id}` | DELETE | 删除考试安排 | ✅ |
| `/api/exam-schedules/{id}/cancel` | PUT | 取消考试安排 | ✅ |

---

## 七、校园公告管理（7个接口）✅ 已实现

**用途**：校园公告的发布、查询、置顶、撤回等管理功能

| 接口路径 | 方法 | 功能说明 | 状态 |
|---------|------|---------|------|
| `/api/announcements` | POST | 发布公告 | ✅ |
| `/api/announcements` | GET | 查询公告列表（多条件过滤、分页） | ✅ |
| `/api/announcements/{id}` | GET | 查询公告详情（自动增加浏览次数） | ✅ |
| `/api/announcements/{id}` | PUT | 更新公告 | ✅ |
| `/api/announcements/{id}` | DELETE | 删除公告 | ✅ |
| `/api/announcements/{id}/top` | PUT | 置顶/取消置顶 | ✅ |
| `/api/announcements/{id}/withdraw` | PUT | 撤回公告 | ✅ |

---

## 八、用户来访登记管理（10个接口）✅ 已实现

**用途**：来访人员的登记、审核、进入/离开登记等全流程管理

| 接口路径 | 方法 | 功能说明 | 状态 |
|---------|------|---------|------|
| `/api/visitors` | POST | 创建来访登记 | ✅ |
| `/api/visitors` | GET | 查询来访登记列表（多条件过滤、分页） | ✅ |
| `/api/visitors/{id}` | GET | 查询来访登记详情 | ✅ |
| `/api/visitors/{id}/approve` | PUT | 审核并批准来访登记 | ✅ |
| `/api/visitors/{id}/reject` | PUT | 拒绝来访登记 | ✅ |
| `/api/visitors/{id}/entry` | PUT | 登记来访人实际进入时间 | ✅ |
| `/api/visitors/{id}/exit` | PUT | 登记来访人实际离开时间 | ✅ |
| `/api/visitors/{id}` | PUT | 更新来访登记 | ✅ |
| `/api/visitors/{id}` | DELETE | 删除来访登记 | ✅ |
| `/api/visitors/export` | GET | 导出来访登记（Excel） | 🟡 |

**权限说明**：
- 系统管理员、后勤人员：可管理所有来访登记
- 其他用户：仅可查询与自己相关的记录

---

## 九、角色权限管理（9个接口）✅ 已实现（6个核心接口）

**用途**：系统角色和权限的管理，实现基于角色的访问控制（RBAC）

| 接口路径 | 方法 | 功能说明 | 状态 |
|---------|------|---------|------|
| `/api/roles` | POST | 创建角色 | ✅ |
| `/api/roles` | GET | 查询角色列表（分页） | ✅ |
| `/api/roles/{id}` | GET | 查询角色详情 | ✅ |
| `/api/roles/{id}` | PUT | 更新角色 | ✅ |
| `/api/roles/{id}` | DELETE | 删除角色 | ✅ |
| `/api/roles/{roleId}/permissions` | POST | 为角色分配权限 | 🟡 |
| `/api/permissions` | GET | 查询权限列表（树形结构） | 🟡 |
| `/api/users/{userId}/roles` | POST | 为用户分配角色 | ❌ |
| `/api/users/{userId}/roles` | GET | 查询用户角色 | ❌ |

---

## 十、数据查询权限管理（5个接口）🟡 部分实现

**用途**：实现权限最小化原则，控制用户可查询的数据范围（全部/年级/班级/自己/自定义）

| 接口路径 | 方法 | 功能说明 | 状态 |
|---------|------|---------|------|
| `/api/data-permissions` | POST | 创建数据查询权限配置 | 🟡 |
| `/api/data-permissions` | GET | 查询数据查询权限列表（分页） | 🟡 |
| `/api/data-permissions/{id}` | GET | 查询数据查询权限详情 | 🟡 |
| `/api/data-permissions/{id}` | PUT | 更新数据查询权限配置 | 🟡 |
| `/api/data-permissions/{id}` | DELETE | 删除数据查询权限配置 | 🟡 |

---

## 十一、教师班级关联管理（3个接口）🟡 部分实现

**用途**：实现教师仅可查询本班级数据的权限控制

| 接口路径 | 方法 | 功能说明 | 状态 |
|---------|------|---------|------|
| `/api/teachers/{teacherId}/classes` | POST | 为教师分配管理的班级 | 🟡 |
| `/api/teachers/{teacherId}/classes` | GET | 查询教师管理的所有班级 | 🟡 |
| `/api/teacher-classes/{id}` | DELETE | 删除教师与班级的关联关系 | 🟡 |

---

## 十二、年级管理员年级关联管理（3个接口）🟡 部分实现

**用途**：实现年级管理员仅可查询本年级数据的权限控制

| 接口路径 | 方法 | 功能说明 | 状态 |
|---------|------|---------|------|
| `/api/grade-admins/{adminId}/grades` | POST | 为年级管理员分配管理的年级 | 🟡 |
| `/api/grade-admins/{adminId}/grades` | GET | 查询年级管理员管理的所有年级 | 🟡 |
| `/api/grade-admin-grades/{id}` | DELETE | 删除年级管理员与年级的关联关系 | 🟡 |

---

## 十三、考勤管理（6个接口）✅ 已实现

**用途**：班级每日考勤汇总和学生异常考勤记录管理（仅记录缺勤/迟到/请假/早退）

| 接口路径 | 方法 | 功能说明 | 状态 |
|---------|------|---------|------|
| `/api/attendance/classes` | GET | 查询班级每日考勤汇总列表 | ✅ |
| `/api/attendance/classes` | POST | 创建或更新班级每日考勤汇总 | ✅ |
| `/api/attendance/students` | GET | 查询学生异常考勤记录列表 | ✅ |
| `/api/attendance/students` | POST | 创建学生异常考勤记录 | ✅ |
| `/api/attendance/students/{id}` | PUT | 更新学生异常考勤记录 | ✅ |
| `/api/attendance/students/{id}` | DELETE | 删除学生异常考勤记录 | ✅ |

---

## 十四、公共批量操作（3个接口）🟡 部分实现

**用途**：通用的批量导入、导出、模板下载功能，支持学生、成绩、考勤等业务类型

| 接口路径 | 方法 | 功能说明 | 状态 |
|---------|------|---------|------|
| `/api/common/upload` | POST | 批量上传文件（学生、成绩、考勤等） | 🟡 |
| `/api/common/template` | GET | 下载批量导入模板 | 🟡 |
| `/api/common/export` | GET | 批量导出数据（学生、成绩、考勤等） | 🟡 |

---

## 十五、统计报表（3个接口）❌ 未实现

**用途**：提供学生、成绩、来访登记等维度的统计分析功能

| 接口路径 | 方法 | 功能说明 | 状态 |
|---------|------|---------|------|
| `/api/statistics/students` | GET | 学生统计（按年级、班级、专业等维度） | ❌ |
| `/api/statistics/scores` | GET | 成绩统计（平均分、及格率等） | ❌ |
| `/api/statistics/visitors` | GET | 来访登记统计 | ❌ |

---

## 十六、文件上传（2个接口）❌ 未实现

**用途**：文件（图片、文档等）的上传和管理功能

| 接口路径 | 方法 | 功能说明 | 状态 |
|---------|------|---------|------|
| `/api/files/upload` | POST | 上传文件（图片、文档等） | ❌ |
| `/api/files/{fileUrl}` | DELETE | 删除已上传的文件 | ❌ |

---

## 权限最小化原则实施

### 数据查询自动过滤
- **教师**：自动过滤，仅返回 `teacher_classes` 表中关联的班级数据
- **年级管理员**：自动过滤，仅返回 `grade_admin_grades` 表中关联的年级数据
- **学生**：自动过滤，仅返回自己的数据

### 数据导出限制
- **教师**：禁止导出（`can_export = 0`）
- **年级管理员**：禁止导出（`can_export = 0`）
- **系统管理员**：允许导出
- **后勤人员**：允许导出（仅限来访登记）

### 字段权限控制
- 通过 `field_permissions` 字段可限制可查询的字段
- 敏感字段（身份证号、家庭地址等）可通过此机制进行保护

---

## 接口设计原则

1. **RESTful风格**：遵循RESTful API设计规范
2. **统一响应格式**：所有接口返回统一的JSON格式
3. **权限验证**：每个接口都进行权限验证
4. **参数验证**：所有输入参数进行验证
5. **异常处理**：统一异常处理机制
6. **日志记录**：重要操作记录日志
7. **数据脱敏**：根据权限对敏感字段脱敏
8. **分页限制**：分页查询限制最大每页数量

---

---

## 实现状态说明

- ✅ **已实现**：接口已完整实现，包含Service层和数据库操作
- 🟡 **部分实现**：接口已创建但Service层为占位实现（TODO）
- ❌ **未实现**：接口尚未创建

---

## 详细文档

- **实现状态详情**：`API_STATUS.md` - 详细的接口实现状态和说明
- **完整接口设计**：`API_DESIGN.md` - 详细的接口设计文档（参数、响应格式等）

