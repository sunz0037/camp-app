package com.camp.app.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 验证数据库表是否创建成功
 */
public class VerifyTables {
    
    private static final String DB_HOST = System.getProperty("db.host", "150.158.115.149");
    private static final String DB_PORT = System.getProperty("db.port", "3306");
    private static final String DB_NAME = System.getProperty("db.name", "camp");
    private static final String DB_USER = System.getProperty("db.user", "campuser");
    private static final String DB_PASSWORD = System.getProperty("db.password", "camp123456");
    
    // 期望的表列表
    private static final String[] EXPECTED_TABLES = {
        "sys_users", "students", "emergency_contacts", "courses", "edu_class_courses",
        "edu_class_course_sessions", "edu_attendance_summaries", "edu_attendance_records",
        "oa_announcements", "oa_announcement_scopes", "oa_announcement_reads",
        "roles", "permissions", "role_permissions", "user_roles",
        "sys_teacher_classes", "sys_affairs_grades", "sys_audit_logs"
    };
    
    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("验证数据库表创建情况");
        System.out.println("==========================================");
        System.out.println("服务器: " + DB_HOST + ":" + DB_PORT);
        System.out.println("数据库: " + DB_NAME);
        System.out.println("==========================================");
        System.out.println();
        
        String url = String.format(
            "jdbc:mysql://%s:%s/%s?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&connectTimeout=10000&socketTimeout=10000",
            DB_HOST, DB_PORT, DB_NAME
        );
        
        Connection conn = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(url, DB_USER, DB_PASSWORD);
            
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SHOW TABLES");
            
            List<String> existingTables = new ArrayList<>();
            while (rs.next()) {
                existingTables.add(rs.getString(1));
            }
            
            System.out.println("数据库中的表（共 " + existingTables.size() + " 个）：");
            for (String table : existingTables) {
                System.out.println("  ✓ " + table);
            }
            System.out.println();
            
            System.out.println("验证期望的表：");
            int successCount = 0;
            int missingCount = 0;
            
            for (String expectedTable : EXPECTED_TABLES) {
                if (existingTables.contains(expectedTable)) {
                    System.out.println("  ✓ " + expectedTable + " - 存在");
                    successCount++;
                } else {
                    System.out.println("  ✗ " + expectedTable + " - 缺失");
                    missingCount++;
                }
            }
            
            System.out.println();
            System.out.println("==========================================");
            if (missingCount == 0) {
                System.out.println("✓ 所有表创建成功！");
                System.out.println("  期望表数: " + EXPECTED_TABLES.length);
                System.out.println("  实际表数: " + existingTables.size());
            } else {
                System.out.println("✗ 部分表缺失");
                System.out.println("  成功: " + successCount + " 个");
                System.out.println("  缺失: " + missingCount + " 个");
            }
            System.out.println("==========================================");
            
            // 验证角色数据
            System.out.println();
            System.out.println("验证初始化数据：");
            rs = stmt.executeQuery("SELECT role_code, role_name FROM roles ORDER BY id");
            int roleCount = 0;
            while (rs.next()) {
                roleCount++;
                System.out.println("  ✓ 角色: " + rs.getString("role_code") + " - " + rs.getString("role_name"));
            }
            System.out.println("  角色总数: " + roleCount);
            
            rs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM permissions");
            if (rs.next()) {
                System.out.println("  权限总数: " + rs.getInt("cnt"));
            }
            
            rs.close();
            stmt.close();
            
        } catch (Exception e) {
            System.err.println("✗ 验证失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

