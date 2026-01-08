package com.camp.app.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 执行数据库建表语句工具
 */
public class ExecuteSchema {
    
    private static final String DB_HOST = System.getProperty("db.host", "150.158.115.149");
    private static final String DB_PORT = System.getProperty("db.port", "3306");
    private static final String DB_NAME = System.getProperty("db.name", "camp");
    private static final String DB_USER = System.getProperty("db.user", "campuser");
    private static final String DB_PASSWORD = System.getProperty("db.password", "camp123456");
    
    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("执行数据库建表语句");
        System.out.println("==========================================");
        System.out.println("服务器: " + DB_HOST + ":" + DB_PORT);
        System.out.println("数据库: " + DB_NAME);
        System.out.println("用户: " + DB_USER);
        System.out.println("==========================================");
        System.out.println();
        
        String url = String.format(
            "jdbc:mysql://%s:%s/%s?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&connectTimeout=30000&socketTimeout=60000&allowMultiQueries=true",
            DB_HOST, DB_PORT, DB_NAME
        );
        
        Connection conn = null;
        Statement stmt = null;
        
        try {
            // 加载驱动
            System.out.println("1. 加载MySQL驱动...");
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("   ✓ 驱动加载成功");
            System.out.println();
            
            // 连接数据库
            System.out.println("2. 连接数据库...");
            conn = DriverManager.getConnection(url, DB_USER, DB_PASSWORD);
            System.out.println("   ✓ 连接成功");
            System.out.println();
            
            // 读取SQL文件
            System.out.println("3. 读取建表语句...");
            List<String> sqlStatements = readSchemaFile();
            System.out.println("   ✓ 读取成功，共 " + sqlStatements.size() + " 条SQL语句");
            System.out.println();
            
            // 执行SQL语句
            System.out.println("4. 执行建表语句...");
            stmt = conn.createStatement();
            
            int successCount = 0;
            int skipCount = 0;
            int errorCount = 0;
            
            for (int i = 0; i < sqlStatements.size(); i++) {
                String sql = sqlStatements.get(i);
                
                // 跳过空语句和注释
                if (sql.trim().isEmpty() || sql.trim().startsWith("--")) {
                    skipCount++;
                    continue;
                }
                
                try {
                    // 执行SQL
                    boolean hasResult = stmt.execute(sql);
                    
                    // 检查是否有结果集（如SELECT语句）
                    if (hasResult) {
                        // 对于INSERT等语句，检查更新行数
                        int updateCount = stmt.getUpdateCount();
                        if (updateCount >= 0) {
                            successCount++;
                            System.out.println("   [" + (i + 1) + "/" + sqlStatements.size() + "] ✓ 执行成功 (影响 " + updateCount + " 行)");
                        } else {
                            successCount++;
                            System.out.println("   [" + (i + 1) + "/" + sqlStatements.size() + "] ✓ 执行成功");
                        }
                    } else {
                        successCount++;
                        int updateCount = stmt.getUpdateCount();
                        if (updateCount > 0) {
                            System.out.println("   [" + (i + 1) + "/" + sqlStatements.size() + "] ✓ 执行成功 (影响 " + updateCount + " 行)");
                        } else {
                            System.out.println("   [" + (i + 1) + "/" + sqlStatements.size() + "] ✓ 执行成功");
                        }
                    }
                } catch (Exception e) {
                    errorCount++;
                    String errorMsg = e.getMessage();
                    // 如果是表已存在的错误，不算错误
                    if (errorMsg != null && (errorMsg.contains("already exists") || 
                                             errorMsg.contains("Duplicate key") ||
                                             errorMsg.contains("Duplicate entry"))) {
                        skipCount++;
                        errorCount--;
                        System.out.println("   [" + (i + 1) + "/" + sqlStatements.size() + "] ⚠ 已存在，跳过: " + 
                                         errorMsg.substring(0, Math.min(50, errorMsg.length())));
                    } else {
                        System.err.println("   [" + (i + 1) + "/" + sqlStatements.size() + "] ✗ 执行失败: " + errorMsg);
                        // 打印SQL的前100个字符以便调试
                        String sqlPreview = sql.length() > 100 ? sql.substring(0, 100) + "..." : sql;
                        System.err.println("   SQL: " + sqlPreview);
                    }
                }
            }
            
            System.out.println();
            System.out.println("==========================================");
            System.out.println("执行完成！");
            System.out.println("成功: " + successCount + " 条");
            System.out.println("跳过: " + skipCount + " 条");
            if (errorCount > 0) {
                System.out.println("失败: " + errorCount + " 条");
            }
            System.out.println("==========================================");
            
        } catch (ClassNotFoundException e) {
            System.err.println("✗ 错误: MySQL驱动未找到");
            System.err.println("  请确保在classpath中包含mysql-connector-j依赖");
            e.printStackTrace();
            System.exit(1);
        } catch (java.sql.SQLException e) {
            System.err.println("✗ 数据库连接失败");
            System.err.println("错误代码: " + e.getErrorCode());
            System.err.println("SQL状态: " + e.getSQLState());
            System.err.println("错误信息: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("✗ 发生未知错误");
            e.printStackTrace();
            System.exit(1);
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (conn != null) {
                    conn.close();
                    System.out.println("连接已关闭");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 读取schema.sql文件
     */
    private static List<String> readSchemaFile() throws Exception {
        List<String> statements = new ArrayList<>();
        StringBuilder currentStatement = new StringBuilder();
        
        // 尝试多种方式加载资源文件
        InputStream is = ExecuteSchema.class.getClassLoader().getResourceAsStream("db/schema.sql");
        if (is == null) {
            is = ExecuteSchema.class.getResourceAsStream("/db/schema.sql");
        }
        if (is == null) {
            is = ClassLoader.getSystemResourceAsStream("db/schema.sql");
        }
        if (is == null) {
            // 尝试从文件系统读取
            java.io.File file = new java.io.File("src/main/resources/db/schema.sql");
            if (file.exists()) {
                is = new java.io.FileInputStream(file);
            }
        }
        if (is == null) {
            throw new Exception("无法找到 db/schema.sql 文件，请确保文件存在于 src/main/resources/db/schema.sql");
        }
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                // 跳过注释行
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                    continue;
                }
                
                // 移除行尾注释
                int commentIndex = trimmed.indexOf("--");
                if (commentIndex > 0) {
                    trimmed = trimmed.substring(0, commentIndex).trim();
                }
                
                if (trimmed.isEmpty()) {
                    continue;
                }
                
                currentStatement.append(trimmed).append(" ");
                
                // 如果以分号结尾，说明是一个完整的SQL语句
                if (trimmed.endsWith(";")) {
                    String sql = currentStatement.toString().trim();
                    if (!sql.isEmpty() && sql.length() > 1) {
                        statements.add(sql);
                    }
                    currentStatement.setLength(0);
                }
            }
            
            // 处理最后一个语句（如果没有以分号结尾）
            String lastSql = currentStatement.toString().trim();
            if (!lastSql.isEmpty() && lastSql.length() > 1) {
                statements.add(lastSql);
            }
        }
        
        return statements;
    }
}

