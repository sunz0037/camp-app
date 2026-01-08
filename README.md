# 校园OA系统

校园OA系统（学生信息管理、学生应急联系人管理、考试成绩管理、校园公告、用户来访登记、数据查询权限管理）

## 项目文档

所有项目文档已统一整理到 `project_doc/` 目录下，请查看：

- **[项目文档目录](./project_doc/README.md)** - 文档导航和分类说明

### 快速链接

- **API文档**: [project_doc/api/](./project_doc/api/)
- **数据库文档**: [project_doc/db/](./project_doc/db/)
- **使用指南**: [project_doc/guides/](./project_doc/guides/)

## 技术栈

- **框架**: Spring Boot 3.2.0
- **数据库**: MySQL 8.0
- **ORM**: Spring Data JPA / Hibernate
- **密码加密**: BCrypt
- **构建工具**: Gradle

## 快速开始

### 1. 数据库初始化

参考 [数据库初始化文档](./project_doc/guides/DATABASE_INIT.md)

### 2. 配置数据库连接

参考 [数据库配置文档](./project_doc/guides/DATABASE_CONFIG.md)

### 3. 启动应用

```bash
./gradlew bootRun
```

### 4. 登录系统

参考 [登录系统使用指南](./project_doc/guides/LOGIN_GUIDE.md)

## 主要功能

- ✅ 三类身份登录（学生、教工、家长/访客）
- ✅ 首次登录激活机制
- ✅ 学生信息管理
- ✅ 考试成绩管理
- ✅ 校园公告
- ✅ 来访登记
- ✅ 考勤管理
- ✅ 权限管理

## 项目结构

```
camp-app/
├── src/
│   ├── main/
│   │   ├── java/com/camp/app/
│   │   │   ├── api/          # API响应模型
│   │   │   ├── config/       # 配置类
│   │   │   ├── controller/   # REST控制器
│   │   │   ├── entity/       # JPA实体
│   │   │   ├── model/        # DTO/VO模型
│   │   │   ├── repository/   # 数据访问层
│   │   │   ├── service/      # 服务层
│   │   │   └── util/         # 工具类
│   │   └── resources/
│   │       ├── db/           # 数据库脚本
│   │       └── application.properties
│   └── test/
├── project_doc/              # 项目文档（统一目录）
└── build.gradle
```

## 开发说明

详细开发文档请查看 [project_doc/](./project_doc/) 目录。

