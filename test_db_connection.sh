#!/bin/bash

# 数据库连接测试脚本

echo "=========================================="
echo "MySQL数据库连接测试"
echo "=========================================="
echo "服务器: 150.158.115.149:3306"
echo "数据库: camp"
echo "用户: campuser"
echo "=========================================="
echo ""

# 1. 测试网络连通性
echo "1. 测试网络连通性..."
if ping -c 2 150.158.115.149 > /dev/null 2>&1; then
    echo "   ✓ 网络连通正常"
else
    echo "   ✗ 网络不通"
    exit 1
fi
echo ""

# 2. 测试端口
echo "2. 测试端口3306..."
if nc -zv -w 5 150.158.115.149 3306 > /dev/null 2>&1; then
    echo "   ✓ 端口3306可访问"
else
    echo "   ✗ 端口3306连接超时"
    echo "   可能原因："
    echo "   - 防火墙阻止了3306端口"
    echo "   - MySQL服务未监听公网IP"
    echo "   - MySQL服务未启动"
    echo ""
    echo "   建议："
    echo "   1. 检查MySQL服务器防火墙配置"
    echo "   2. 检查MySQL bind-address配置（应为0.0.0.0）"
    echo "   3. 如果可能，使用内网地址：10.0.0.3:3306"
    exit 1
fi
echo ""

# 3. 尝试使用Java测试连接
echo "3. 使用Java JDBC测试连接..."
cd /Users/sunz/AIProject/camp-app
./gradlew compileJava > /dev/null 2>&1

# 创建简单的测试类
cat > /tmp/TestConnection.java << 'EOF'
import java.sql.*;
public class TestConnection {
    public static void main(String[] args) {
        String url = "jdbc:mysql://150.158.115.149:3306/camp?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&connectTimeout=10000";
        String user = "campuser";
        String password = "camp123456";
        
        try {
            System.out.println("   正在连接...");
            Connection conn = DriverManager.getConnection(url, user, password);
            System.out.println("   ✓ 连接成功！");
            
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT DATABASE(), USER(), VERSION()");
            if (rs.next()) {
                System.out.println("   当前数据库: " + rs.getString(1));
                System.out.println("   当前用户: " + rs.getString(2));
                System.out.println("   MySQL版本: " + rs.getString(3));
            }
            conn.close();
        } catch (Exception e) {
            System.out.println("   ✗ 连接失败: " + e.getMessage());
            System.exit(1);
        }
    }
}
EOF

# 编译并运行
javac -cp "build/classes/java/main:$(find ~/.gradle -name 'mysql-connector-j*.jar' | head -1)" /tmp/TestConnection.java 2>/dev/null
if [ $? -eq 0 ]; then
    java -cp "/tmp:build/classes/java/main:$(find ~/.gradle -name 'mysql-connector-j*.jar' | head -1)" TestConnection 2>&1
else
    echo "   无法编译测试程序（需要MySQL驱动）"
fi

echo ""
echo "=========================================="

