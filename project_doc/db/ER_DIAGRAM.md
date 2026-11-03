# 校园OA系统数据库ER图

首期表关系见 [PHASE1_SCHEMA.md](./PHASE1_SCHEMA.md)。下图是旧结构。

## Mermaid ER图

```mermaid
erDiagram
    users ||--o| students : "一对一"
    users ||--o{ courses : "一对多(教师)"
    users ||--o{ announcements : "一对多(发布人)"
    users ||--o{ visitor_registrations : "一对多(访问目标)"
    users ||--o{ visitor_registrations : "一对多(接待人)"
    users ||--o{ user_roles : "一对多"
    users ||--o{ data_query_permissions : "一对多"
    users ||--o{ teacher_classes : "一对多(教师)"
    users ||--o{ grade_admin_grades : "一对多(年级管理员)"
    students ||--o{ student_attendance_records : "一对多(每日考勤)"
    
    students ||--o{ emergency_contacts : "一对多"
    students ||--o{ exam_scores : "一对多"
    
    courses ||--o{ exam_scores : "一对多"
    
    roles ||--o{ user_roles : "一对多"
    roles ||--o{ role_permissions : "一对多"
    roles ||--o{ data_query_permissions : "一对多"
    
    permissions ||--o{ role_permissions : "一对多"
    
    users {
        bigint id PK
        varchar username UK
        varchar password
        varchar real_name
        varchar id_card
        varchar phone
        varchar email
        tinyint user_type
        tinyint status
        datetime created_at
        datetime updated_at
    }
    
    students {
        bigint id PK
        bigint user_id FK
        varchar student_no UK
        varchar grade
        varchar class_name
        varchar major
        varchar department
        date enrollment_date
        tinyint status
        datetime created_at
        datetime updated_at
    }
    
    emergency_contacts {
        bigint id PK
        bigint student_id FK
        varchar contact_name
        varchar relationship
        varchar phone
        tinyint is_primary
        datetime created_at
        datetime updated_at
    }
    
    courses {
        bigint id PK
        varchar course_code UK
        varchar course_name
        decimal credit
        bigint teacher_id FK
        varchar semester
        varchar academic_year
        datetime created_at
        datetime updated_at
    }
    
    exam_scores {
        bigint id PK
        bigint student_id FK
        bigint course_id FK
        varchar exam_type
        decimal score
        decimal grade_point
        varchar semester
        varchar academic_year
        datetime created_at
        datetime updated_at
    }
    
    announcements {
        bigint id PK
        varchar title
        text content
        varchar category
        bigint publisher_id FK
        datetime publish_time
        datetime expire_time
        tinyint is_top
        tinyint is_urgent
        int view_count
        tinyint status
        datetime created_at
        datetime updated_at
    }
    
    visitor_registrations {
        bigint id PK
        varchar visitor_name
        varchar visitor_phone
        varchar visit_purpose
        bigint visit_target_user_id FK
        date visit_date
        datetime entry_time
        datetime exit_time
        bigint receptionist_id FK
        tinyint status
        datetime created_at
        datetime updated_at
    }
    
    roles {
        bigint id PK
        varchar role_code UK
        varchar role_name
        varchar description
        tinyint status
        datetime created_at
        datetime updated_at
    }
    
    permissions {
        bigint id PK
        varchar permission_code UK
        varchar permission_name
        varchar resource_type
        varchar resource_path
        bigint parent_id
        varchar description
        tinyint status
        datetime created_at
        datetime updated_at
    }
    
    role_permissions {
        bigint id PK
        bigint role_id FK
        bigint permission_id FK
        datetime created_at
    }
    
    user_roles {
        bigint id PK
        bigint user_id FK
        bigint role_id FK
        datetime created_at
    }
    
    data_query_permissions {
        bigint id PK
        bigint user_id FK
        bigint role_id FK
        varchar data_type
        varchar query_scope
        varchar scope_value
        text field_permissions
        tinyint can_export
        tinyint can_print
        tinyint status
        datetime created_at
        datetime updated_at
    }
    
    teacher_classes {
        bigint id PK
        bigint teacher_id FK
        varchar grade
        varchar class_name
        varchar subject
        tinyint is_class_teacher
        datetime created_at
        datetime updated_at
    }
    
    grade_admin_grades {
        bigint id PK
        bigint admin_id FK
        varchar grade
        varchar department
        varchar description
        datetime created_at
        datetime updated_at
    }
    
    class_attendance_daily {
        bigint id PK
        date attendance_date
        varchar grade
        varchar class_name
        int expected_count
        int present_count
        int absent_count
        int late_count
        int leave_count
        bigint recorder_id FK
        varchar remark
        datetime created_at
        datetime updated_at
    }
    
    student_attendance_records {
        bigint id PK
        bigint student_id FK
        date attendance_date
        varchar grade
        varchar class_name
        tinyint status  // 1-缺勤 2-迟到 3-请假 4-早退（仅记录异常）
        varchar reason_type
        varchar reason_detail
        bigint notified_contact_id FK
        varchar notified_contact_name
        varchar notified_method
        datetime notified_at
        varchar attachment_url
        bigint recorder_id FK
        datetime created_at
        datetime updated_at
    }
```

## 表关系说明

### 1. 用户与业务实体关系
- **users → students**: 一对一关系，每个学生用户对应一条学生信息记录
- **users → courses**: 一对多关系，一个教师可以教授多门课程
- **users → announcements**: 一对多关系，一个用户可以发布多条公告
- **users → visitor_registrations**: 一对多关系（作为访问目标或接待人）

### 2. 学生相关关系
- **students → emergency_contacts**: 一对多关系，一个学生可以有多个应急联系人
- **students → exam_scores**: 一对多关系，一个学生有多条成绩记录

### 3. 课程与成绩关系
- **courses → exam_scores**: 一对多关系，一门课程有多条成绩记录

### 4. 权限管理关系
- **users ↔ user_roles ↔ roles**: 多对多关系，用户可以有多个角色
- **roles ↔ role_permissions ↔ permissions**: 多对多关系，角色可以有多个权限
- **users/roles → data_query_permissions**: 一对多关系，用户或角色可以有多个数据查询权限
- **users → teacher_classes**: 一对多关系，一个教师可以管理多个班级（用于权限控制，遵循权限最小化原则）
- **users → grade_admin_grades**: 一对多关系，一个年级管理员可以管理多个年级（用于权限控制，遵循权限最小化原则）
- **students → student_attendance_records**: 一对多关系，一个学生有多条每日考勤记录

## 核心业务流程

### 学生信息管理流程
```
用户注册 → users表
    ↓
创建学生信息 → students表
    ↓
添加应急联系人 → emergency_contacts表
```

### 成绩管理流程
```
创建课程 → courses表
    ↓
录入成绩 → exam_scores表（关联学生和课程）
```

### 权限管理流程（遵循权限最小化原则）
```
创建角色 → roles表
    ↓
分配权限 → role_permissions表
    ↓
用户分配角色 → user_roles表
    ↓
配置数据查询权限 → data_query_permissions表
    ↓
教师关联班级 → teacher_classes表（教师仅可查询本班级）
    ↓
年级管理员关联年级 → grade_admin_grades表（年级管理员仅可查询本年级）
```

### 权限最小化原则说明
- **教师角色**：通过 `teacher_classes` 表关联，仅可查询所管理班级的学生信息、成绩、应急联系人，不可导出和打印
- **年级管理员角色**：通过 `grade_admin_grades` 表关联，仅可查询所管理年级的学生信息、成绩、应急联系人，不可导出和打印
- **学生角色**：仅可查询自己的信息
- **后勤人员角色**：可管理来访登记（原安保人员角色）
- **系统管理员**：拥有所有权限

### 来访登记流程
```
来访登记 → visitor_registrations表
    ↓
审核批准 → 更新status
    ↓
进入登记 → 记录entry_time
    ↓
离开登记 → 记录exit_time
```

