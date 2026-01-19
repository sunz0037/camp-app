# 校园OA系统权限与角色逻辑梳理

## 一、用户类型（User Type）

系统定义了三种基础用户类型，存储在 `users` 表的 `user_type` 字段：

| 类型值 | 类型名称 | 说明 |
|--------|---------|------|
| 1 | 学生 | 学生用户，通过学号登录 |
| 2 | 教职工 | 教职工用户，通过工号登录 |
| 3 | 管理员 | 系统管理员用户 |

**注意**：用户类型是基础分类，与角色（Role）是分离的。一个用户可以有多个角色。

---

## 二、角色（Role）

系统定义了5种角色，存储在 `roles` 表中：

| 角色代码 | 角色名称 | 说明 |
|---------|---------|------|
| ADMIN | 系统管理员 | 拥有系统所有权限 |
| TEACHER | 教师 | 教师角色，仅可查询本班级相关信息（权限最小化原则） |
| GRADE_ADMIN | 年级管理员 | 年级管理员角色，仅可查询该年级相关信息（权限最小化原则） |
| STUDENT | 学生 | 学生角色，可查看个人信息和成绩 |
| LOGISTICS | 后勤人员 | 后勤人员，可管理来访登记 |

### 角色与用户的关联

- **关联表**：`user_roles`
- **关系**：多对多（一个用户可以有多个角色，一个角色可以分配给多个用户）
- **唯一约束**：`(user_id, role_id)` 唯一

---

## 三、权限（Permission）

系统定义了功能权限，存储在 `permissions` 表中，用于控制用户对系统功能的访问。

### 权限结构

| 字段 | 说明 |
|------|------|
| permission_code | 权限代码（唯一标识） |
| permission_name | 权限名称 |
| resource_type | 资源类型：menu-菜单，button-按钮，data-数据 |
| resource_path | 资源路径（如API路径） |
| parent_id | 父权限ID（支持权限树形结构） |
| description | 权限描述 |
| sort_order | 排序顺序 |
| status | 状态：0-禁用，1-启用 |

### 系统预定义权限

根据 `schema.sql`，系统预定义了以下权限：

| 权限代码 | 权限名称 | 资源类型 | 资源路径 | 说明 |
|---------|---------|---------|---------|------|
| student:view | 查看学生信息 | data | /api/students | 查看学生信息权限 |
| student:edit | 编辑学生信息 | data | /api/students | 编辑学生信息权限 |
| student:delete | 删除学生信息 | data | /api/students | 删除学生信息权限 |
| score:view | 查看成绩 | data | /api/scores | 查看成绩权限 |
| score:edit | 编辑成绩 | data | /api/scores | 编辑成绩权限 |
| announcement:view | 查看公告 | data | /api/announcements | 查看公告权限 |
| announcement:publish | 发布公告 | data | /api/announcements | 发布公告权限 |
| visitor:view | 查看来访登记 | data | /api/visitors | 查看来访登记权限 |
| visitor:manage | 管理来访登记 | data | /api/visitors | 管理来访登记权限 |
| permission:manage | 权限管理 | data | /api/permissions | 权限管理权限 |

### 角色与权限的关联

- **关联表**：`role_permissions`
- **关系**：多对多（一个角色可以有多个权限，一个权限可以分配给多个角色）
- **唯一约束**：`(role_id, permission_id)` 唯一

**注意**：当前 `RoleServiceImpl.assignPermissions()` 方法尚未实现，需要创建 `RolePermission` 实体和 Repository。

---

## 四、数据查询权限（Data Query Permission）

数据查询权限是更细粒度的权限控制，用于控制用户对数据的查询范围，存储在 `data_query_permissions` 表中。

### 数据查询权限结构

| 字段 | 说明 |
|------|------|
| user_id | 用户ID（为空表示角色权限） |
| role_id | 角色ID（为空表示用户权限） |
| data_type | 数据类型：student-学生信息，score-成绩，announcement-公告，visitor-来访登记等 |
| query_scope | 查询范围：all-全部，own-仅自己，department-本部门，grade-本年级，class-本班级，custom-自定义 |
| scope_value | 范围值（当query_scope为custom/grade/class时使用，JSON格式） |
| field_permissions | 字段权限（JSON格式，控制可查询的字段，空表示全部字段） |
| can_export | 是否可导出：0-否，1-是 |
| can_print | 是否可打印：0-否，1-是 |
| status | 状态：0-禁用，1-启用 |

**约束**：`user_id` 和 `role_id` 至少有一个不为空。

### 系统预定义数据查询权限

根据 `schema.sql`，系统为各角色预定义了数据查询权限：

#### 1. 教师角色（TEACHER）

| 数据类型 | 查询范围 | 可导出 | 可打印 | 说明 |
|---------|---------|--------|--------|------|
| student | class | 否 | 否 | 仅可查询本班级学生信息 |
| score | class | 否 | 否 | 仅可查询本班级成绩 |
| emergency_contact | class | 否 | 否 | 仅可查询本班级应急联系人 |

**权限控制实现**：
- 通过 `teacher_classes` 表关联教师与班级
- 教师只能查询其管理的班级的数据

#### 2. 年级管理员角色（GRADE_ADMIN）

| 数据类型 | 查询范围 | 可导出 | 可打印 | 说明 |
|---------|---------|--------|--------|------|
| student | grade | 否 | 否 | 仅可查询该年级学生信息 |
| score | grade | 否 | 否 | 仅可查询该年级成绩 |
| emergency_contact | grade | 否 | 否 | 仅可查询该年级应急联系人 |

**权限控制实现**：
- 通过 `grade_admin_grades` 表关联年级管理员与年级
- 年级管理员只能查询其管理的年级的数据

#### 3. 学生角色（STUDENT）

| 数据类型 | 查询范围 | 可导出 | 可打印 | 说明 |
|---------|---------|--------|--------|------|
| student | own | 否 | 否 | 仅可查询自己的信息 |
| score | own | 否 | 否 | 仅可查询自己的成绩 |

**权限控制实现**：
- 学生只能查询自己的数据（通过 `user_id` 或 `student_id` 过滤）

#### 4. 后勤人员角色（LOGISTICS）

| 数据类型 | 查询范围 | 可导出 | 可打印 | 说明 |
|---------|---------|--------|--------|------|
| visitor | all | 是 | 是 | 可查看和管理所有来访登记 |

**权限控制实现**：
- 后勤人员可以查看和管理所有来访登记，并可导出和打印

#### 5. 系统管理员角色（ADMIN）

**说明**：系统管理员默认拥有所有权限，无需在 `data_query_permissions` 表中配置。

---

## 五、权限控制实现方式

### 1. 基于用户类型的权限控制

在 `AuthServiceImpl` 中，通过 `user_type` 字段区分用户身份：
- `user_type = 1`：学生
- `user_type = 2`：教职工
- `user_type = 3`：管理员

### 2. 基于角色的权限控制

**当前状态**：
- 角色和权限的关联表已定义（`role_permissions`）
- 用户和角色的关联表已定义（`user_roles`）
- 但 `RoleServiceImpl.assignPermissions()` 方法尚未实现（TODO）

**需要实现**：
- 创建 `RolePermission` 实体类
- 创建 `RolePermissionRepository`
- 实现 `RoleServiceImpl.assignPermissions()` 方法
- 实现权限验证逻辑（在 Service 层或通过拦截器/过滤器）

### 3. 基于数据查询权限的控制

**当前状态**：
- `data_query_permissions` 表已定义
- `DataPermissionService` 接口已定义
- 但 `DataPermissionServiceImpl` 的实现为占位实现（TODO）

**需要实现**：
- 实现 `DataPermissionServiceImpl` 的 CRUD 方法
- 在 Service 层实现数据过滤逻辑（如 `StudentServiceImpl` 中的权限过滤）

### 4. 基于关联表的权限控制

#### 教师班级关联（teacher_classes）

- **用途**：控制教师只能查询其管理的班级的数据
- **关联字段**：`teacher_id`, `grade`, `class_name`, `subject`, `is_class_teacher`
- **权限控制**：在查询学生、成绩等数据时，通过 `teacher_id` 和班级信息过滤

#### 年级管理员年级关联（grade_admin_grades）

- **用途**：控制年级管理员只能查询其管理的年级的数据
- **关联字段**：`admin_id`, `grade`, `department`, `description`
- **权限控制**：在查询学生、成绩等数据时，通过 `admin_id` 和年级信息过滤

---

## 六、权限验证流程

### 当前实现状态

1. **登录验证**：✅ 已实现
   - 通过 `AuthServiceImpl.login()` 验证用户身份
   - 返回用户信息和 Token

2. **用户类型验证**：✅ 部分实现
   - 在 `StudentServiceImpl` 等 Service 中，通过 `user_type` 进行基础验证

3. **角色权限验证**：❌ 未实现
   - `RoleServiceImpl.assignPermissions()` 为 TODO
   - 缺少权限验证拦截器/过滤器

4. **数据查询权限验证**：🟡 部分实现
   - `DataPermissionServiceImpl` 为占位实现
   - 部分 Service 层有权限过滤逻辑（如 `StudentServiceImpl`）

### 建议的权限验证流程

```
1. 用户登录
   ↓
2. 获取用户信息（包含 user_type）
   ↓
3. 查询用户角色（user_roles 表）
   ↓
4. 查询角色权限（role_permissions 表）
   ↓
5. 查询数据查询权限（data_query_permissions 表）
   ↓
6. 在 Service 层应用权限过滤
   - 根据 user_type 判断基础权限
   - 根据角色权限判断功能权限
   - 根据数据查询权限过滤数据范围
   - 根据 teacher_classes 或 grade_admin_grades 过滤数据
```

---

## 七、权限划分总结

### 功能权限（Permission）

通过 `permissions` 表和 `role_permissions` 表控制用户对系统功能的访问，如：
- 查看、编辑、删除学生信息
- 查看、编辑成绩
- 查看、发布公告
- 管理来访登记
- 权限管理

### 数据权限（Data Query Permission）

通过 `data_query_permissions` 表控制用户对数据的查询范围，如：
- 全部数据（all）
- 仅自己（own）
- 本年级（grade）
- 本班级（class）
- 自定义范围（custom）

### 关联权限（Association Permission）

通过关联表控制用户对特定数据的访问，如：
- `teacher_classes`：教师只能访问其管理的班级
- `grade_admin_grades`：年级管理员只能访问其管理的年级

---

## 八、各角色权限矩阵

| 角色 | 学生信息 | 成绩 | 公告 | 来访登记 | 权限管理 | 数据范围 |
|------|---------|------|------|---------|---------|---------|
| ADMIN | ✅ 全部 | ✅ 全部 | ✅ 全部 | ✅ 全部 | ✅ 全部 | 全部数据 |
| TEACHER | ✅ 查看/编辑 | ✅ 查看/编辑 | ✅ 查看 | ❌ | ❌ | 本班级 |
| GRADE_ADMIN | ✅ 查看/编辑 | ✅ 查看/编辑 | ✅ 查看 | ❌ | ❌ | 本年级 |
| STUDENT | ✅ 查看（自己） | ✅ 查看（自己） | ✅ 查看 | ❌ | ❌ | 仅自己 |
| LOGISTICS | ❌ | ❌ | ❌ | ✅ 全部 | ❌ | 来访登记全部 |

**说明**：
- ✅ 表示有权限
- ❌ 表示无权限
- 数据范围列表示该角色可以访问的数据范围

---

## 九、待实现功能

1. **角色权限分配功能**
   - 实现 `RoleServiceImpl.assignPermissions()` 方法
   - 创建 `RolePermission` 实体和 Repository
   - 实现权限验证拦截器/过滤器

2. **数据查询权限功能**
   - 实现 `DataPermissionServiceImpl` 的所有方法
   - 在 Service 层集成数据权限过滤逻辑

3. **权限验证机制**
   - 实现统一的权限验证拦截器
   - 在 Controller 层或 Service 层进行权限检查
   - 实现基于 JWT Token 的权限验证（当前使用简单的 `X-User-Id` 请求头）

4. **用户角色分配功能**
   - 实现用户角色分配接口（`/api/users/{userId}/roles`）
   - 在用户管理页面支持角色分配

---

## 十、权限设计原则

1. **权限最小化原则**：用户只能访问其工作所需的最小数据范围
2. **角色分离原则**：用户类型和角色分离，一个用户可以有多个角色
3. **数据隔离原则**：通过关联表和数据查询权限实现数据隔离
4. **可扩展性原则**：权限系统设计支持灵活的权限配置和扩展

---

**文档版本**：v1.0  
**最后更新**：2024年12月
