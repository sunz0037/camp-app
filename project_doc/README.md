# 项目文档目录

本目录包含校园OA系统的所有项目文档。

## 目录结构

```
project_doc/
├── README.md                    # 本文档
├── api/                         # API接口文档
│   ├── API_DESIGN.md           # API设计文档
│   ├── API_STATUS.md           # API实现状态
│   └── API_SUMMARY.md          # API接口摘要
├── db/                          # 数据库文档
│   ├── CHANGELOG.md            # 数据库变更日志
│   ├── ER_DIAGRAM.md           # ER图
│   ├── INSERT_STUDENTS_README.md # 学生数据插入说明
│   └── README.md               # 数据库设计文档
├── guides/                      # 使用指南
│   ├── BCRYPT_USAGE.md         # BCrypt密码加密使用说明
│   ├── CONNECTION_STATUS.md    # 数据库连接状态报告
│   ├── DATABASE_CONFIG.md     # 数据库配置说明
│   ├── DATABASE_INIT.md        # 数据库初始化说明
│   ├── EXECUTE_SCHEMA.md      # 执行建表语句说明
│   ├── IDE_SETUP.md           # IDE设置说明
│   ├── LOGIN_GUIDE.md         # 登录系统使用指南
│   ├── PASSWORD_ENCRYPTION.md # 密码加密方案推荐
│   ├── STARTUP_INFO.md        # 启动信息说明
│   └── TROUBLESHOOTING.md     # 故障排查指南
└── config/                      # 配置文件说明（预留）
```

## 文档分类

### 功能状态文档
- **FEATURES_STATUS.md**: 功能与接口实现状态总览，包含后端接口和前端页面的实现情况

### API文档 (api/)
- **API_DESIGN.md**: 详细的API接口设计文档，包含所有接口的请求/响应格式
- **API_STATUS.md**: API接口实现状态跟踪，标记哪些接口已实现、部分实现或未实现
- **API_SUMMARY.md**: API接口清单摘要，快速查看所有接口

### 数据库文档 (db/)
- **README.md**: 数据库设计文档，包含表结构说明、关系图等
- **CHANGELOG.md**: 数据库变更日志，记录所有表结构变更
- **ER_DIAGRAM.md**: 数据库ER图（Mermaid格式）
- **INSERT_STUDENTS_README.md**: 学生数据插入脚本使用说明

### 使用指南 (guides/)
- **LOGIN_GUIDE.md**: 登录系统使用指南，包含三类身份登录流程
- **BCRYPT_USAGE.md**: BCrypt密码加密使用说明
- **DATABASE_CONFIG.md**: 数据库连接配置说明
- **DATABASE_INIT.md**: 数据库初始化步骤
- **EXECUTE_SCHEMA.md**: 执行建表语句的方法
- **CONNECTION_STATUS.md**: 数据库连接状态报告和故障排查
- **TROUBLESHOOTING.md**: 通用故障排查指南
- **STARTUP_INFO.md**: 应用启动信息说明
- **IDE_SETUP.md**: IDE开发环境设置说明
- **PASSWORD_ENCRYPTION.md**: 密码加密方案推荐文档

## 快速导航

### 新手入门
1. 阅读 [README.md](./README.md) 了解项目概述
2. 查看 [DATABASE_INIT.md](./guides/DATABASE_INIT.md) 初始化数据库
3. 参考 [LOGIN_GUIDE.md](./guides/LOGIN_GUIDE.md) 了解登录系统

### 开发参考
- API开发：查看 [api/](./api/) 目录下的文档
- 数据库设计：查看 [db/](./db/) 目录下的文档
- 功能使用：查看 [guides/](./guides/) 目录下的文档

### 故障排查
- 数据库连接问题：查看 [CONNECTION_STATUS.md](./guides/CONNECTION_STATUS.md)
- 通用问题：查看 [TROUBLESHOOTING.md](./guides/TROUBLESHOOTING.md)

## 文档更新

所有文档应保持最新状态，如有变更请及时更新对应的文档。

## 注意事项

- 所有文档使用Markdown格式
- 文档中的代码示例应保持可执行
- 重要变更应在CHANGELOG中记录
