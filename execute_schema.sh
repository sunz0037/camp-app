#!/bin/bash

# 执行数据库建表语句脚本

echo "=========================================="
echo "执行数据库建表语句"
echo "=========================================="

# 配置信息
DB_HOST="${DB_HOST:-150.158.115.149}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-camp}"
DB_USER="${DB_USER:-campuser}"
DB_PASSWORD="${DB_PASSWORD:-camp123456}"

echo "服务器: $DB_HOST:$DB_PORT"
echo "数据库: $DB_NAME"
echo "用户: $DB_USER"
echo "=========================================="
echo ""

# 检查是否使用内网地址
if [ "$DB_HOST" = "150.158.115.149" ]; then
    echo "提示: 当前使用公网地址，如果连接失败，可以尝试使用内网地址："
    echo "  export DB_HOST=10.0.0.3"
    echo "  ./execute_schema.sh"
    echo ""
fi

# 查找MySQL驱动
MYSQL_JAR=$(find ~/.gradle -name "mysql-connector-j*.jar" 2>/dev/null | head -1)

if [ -z "$MYSQL_JAR" ]; then
    echo "错误: 未找到MySQL驱动"
    echo "请先运行: cd /Users/sunz/AIProject/camp-app && ./gradlew build"
    exit 1
fi

# 编译Java类
echo "编译Java类..."
cd /Users/sunz/AIProject/camp-app
./gradlew compileJava > /dev/null 2>&1

if [ $? -ne 0 ]; then
    echo "错误: 编译失败"
    exit 1
fi

echo "✓ 编译成功"
echo ""

# 执行建表语句
echo "执行建表语句..."
java -cp "build/classes/java/main:$MYSQL_JAR" \
     -Ddb.host="$DB_HOST" \
     -Ddb.port="$DB_PORT" \
     -Ddb.name="$DB_NAME" \
     -Ddb.user="$DB_USER" \
     -Ddb.password="$DB_PASSWORD" \
     com.camp.app.util.ExecuteSchema

exit_code=$?

if [ $exit_code -eq 0 ]; then
    echo ""
    echo "=========================================="
    echo "✓ 建表语句执行完成！"
    echo "=========================================="
else
    echo ""
    echo "=========================================="
    echo "✗ 执行失败"
    echo "=========================================="
    echo ""
    echo "如果连接失败，请尝试："
    echo "1. 使用内网地址: export DB_HOST=10.0.0.3 && ./execute_schema.sh"
    echo "2. 检查数据库服务器是否已开放3306端口"
    echo "3. 检查防火墙和安全组配置"
fi

exit $exit_code

