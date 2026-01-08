package com.camp.app.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

/**
 * 数据库连接测试工具
 */
public class DatabaseConnectionTest {
    
    private static final String DB_HOST = "150.158.115.149";
    private static final String DB_PORT = "3306";
    private static final String DB_NAME = "camp";
    private static final String DB_USER = "campuser";
    private static final String DB_PASSWORD = "camp123456";
    
    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("MySQL数据库连接测试");
        System.out.println("==========================================");
        System.out.println("服务器: " + DB_HOST + ":" + DB_PORT);
        System.out.println("数据库: " + DB_NAME);
        System.out.println("用户: " + DB_USER);
        System.out.println("==========================================");
        System.out.println();
        
        String url = String.format(
            "jdbc:mysql://%s:%s/%s?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&connectTimeout=10000&socketTimeout=10000",
            DB_HOST, DB_PORT, DB_NAME
        );
        
        Connection conn = null;
        try {
            System.out.println("1. 加载MySQL驱动...");
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("   ✓ 驱动加载成功");
            System.out.println();
            
            System.out.println("2. 尝试连接数据库...");
            System.out.println("   URL: " + url.replace(DB_PASSWORD, "***"));
            
            long startTime = System.currentTimeMillis();
            conn = DriverManager.getConnection(url, DB_USER, DB_PASSWORD);
            long endTime = System.currentTimeMillis();
            
            System.out.println("   ✓ 连接成功！耗时: " + (endTime - startTime) + "ms");
            System.out.println();
            
            System.out.println("3. 测试数据库查询...");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT 1 AS test, DATABASE() AS db_name, USER() AS db_user, VERSION() AS db_version");
            
            if (rs.next()) {
                System.out.println("   ✓ 查询成功");
                System.out.println("   测试值: " + rs.getInt("test"));
                System.out.println("   当前数据库: " + rs.getString("db_name"));
                System.out.println("   当前用户: " + rs.getString("db_user"));
                System.out.println("   MySQL版本: " + rs.getString("db_version"));
            }
            System.out.println();
            
            System.out.println("4. 查询数据库表...");
            rs = stmt.executeQuery("SHOW TABLES");
            int tableCount = 0;
            System.out.println("   数据库中的表：");
            while (rs.next()) {
                tableCount++;
                System.out.println("   - " + rs.getString(1));
            }
            System.out.println("   总计: " + tableCount + " 个表");
            System.out.println();
            
            System.out.println("==========================================");
            System.out.println("✓ 所有测试通过！数据库连接正常。");
            System.out.println("==========================================");
            
            rs.close();
            stmt.close();
            
        } catch (ClassNotFoundException e) {
            System.err.println("✗ 错误: MySQL驱动未找到");
            System.err.println("  请确保在classpath中包含mysql-connector-j依赖");
            e.printStackTrace();
        } catch (java.sql.SQLException e) {
            System.err.println("✗ 数据库连接失败");
            System.err.println("错误代码: " + e.getErrorCode());
            System.err.println("SQL状态: " + e.getSQLState());
            System.err.println("错误信息: " + e.getMessage());
            System.err.println();
            System.err.println("可能的原因：");
            System.err.println("1. 网络不通或端口3306被防火墙阻止");
            System.err.println("2. MySQL服务未启动");
            System.err.println("3. 用户名或密码错误");
            System.err.println("4. 用户没有远程连接权限");
            System.err.println("5. 数据库不存在");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("✗ 发生未知错误");
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                    System.out.println("连接已关闭");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

