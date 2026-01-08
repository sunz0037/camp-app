#!/bin/bash

# 校园OA系统数据库初始化脚本
# 使用MySQL的camp库，时区：北京时间（Asia/Shanghai）

# 数据库连接信息
DB_HOST="150.158.115.149"
DB_PORT="3306"
DB_USER="campuser"
DB_PASSWORD="camp123456"
DB_NAME="camp"

# SQL脚本路径
SQL_SCRIPT="src/main/resources/db/schema.sql"

echo "=========================================="
echo "校园OA系统数据库初始化"
echo "=========================================="
echo "数据库服务器: ${DB_HOST}:${DB_PORT}"
echo "数据库名称: ${DB_NAME}"
echo "时区: 北京时间 (Asia/Shanghai)"
echo "=========================================="

# 检查mysql命令是否存在
if ! command -v mysql &> /dev/null; then
    echo "错误: 未找到mysql命令，请先安装MySQL客户端"
    echo ""
    echo "安装方法："
    echo "  macOS: brew install mysql-client"
    echo "  Ubuntu/Debian: sudo apt-get install mysql-client"
    echo "  CentOS/RHEL: sudo yum install mysql"
    exit 1
fi

# 创建数据库
echo "正在创建数据库 ${DB_NAME}..."
mysql -h ${DB_HOST} -P ${DB_PORT} -u ${DB_USER} -p${DB_PASSWORD} \
    -e "CREATE DATABASE IF NOT EXISTS ${DB_NAME} DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;" 2>&1

if [ $? -ne 0 ]; then
    echo "错误: 创建数据库失败"
    exit 1
fi

echo "数据库创建成功！"

# 执行建表语句
echo "正在执行建表语句..."
mysql -h ${DB_HOST} -P ${DB_PORT} -u ${DB_USER} -p${DB_PASSWORD} ${DB_NAME} < ${SQL_SCRIPT} 2>&1

if [ $? -ne 0 ]; then
    echo "错误: 执行建表语句失败"
    exit 1
fi

echo "建表语句执行成功！"
echo ""
echo "=========================================="
echo "数据库初始化完成！"
echo "=========================================="
echo "数据库: ${DB_NAME}"
echo "表数量: $(mysql -h ${DB_HOST} -P ${DB_PORT} -u ${DB_USER} -p${DB_PASSWORD} ${DB_NAME} -e "SHOW TABLES;" 2>/dev/null | wc -l | tr -d ' ')"
echo ""
echo "可以启动应用了："
echo "  ./gradlew bootRun"

