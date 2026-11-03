package com.camp.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;

/**
 * dev：用 Flyway 的启动钩子执行仓根 V1 与 seed。
 * V2 只服务已有 MySQL 旧库，空 H2 跑完 V1 后不再执行。
 */
@Configuration
@Profile("dev")
public class DevFlywayConfig {
    @Bean
    public org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy devSchemaStrategy(DataSource dataSource) {
        return flyway -> {
            try (Connection connection = dataSource.getConnection()) {
                if (!tableExists(connection, "sys_users")) {
                    String v1 = Files.readString(Path.of("db/migration/V1__phase1_schema.sql"));
                    ScriptUtils.executeSqlScript(connection, new ByteArrayResource(H2Phase1Scripts.adapt(v1).getBytes(StandardCharsets.UTF_8)));
                }
                String seed = Files.readString(Path.of("db/seed/phase1_demo.sql"));
                ScriptUtils.executeSqlScript(connection, new ByteArrayResource(
                        H2Phase1Scripts.adaptSeed(seed).getBytes(StandardCharsets.UTF_8)));
            } catch (Exception ex) {
                throw new IllegalStateException("执行 db/migration 或 db/seed 失败", ex);
            }
        };
    }

    private boolean tableExists(Connection connection, String table) throws Exception {
        try (ResultSet rs = connection.getMetaData().getTables(null, null, table.toUpperCase(), new String[]{"TABLE"})) {
            if (rs.next()) {
                return true;
            }
        }
        try (ResultSet rs = connection.getMetaData().getTables(null, null, table.toLowerCase(), new String[]{"TABLE"})) {
            return rs.next();
        }
    }
}
