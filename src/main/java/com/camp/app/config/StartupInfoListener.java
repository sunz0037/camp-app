package com.camp.app.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;

/**
 * 应用启动信息监听器
 * 在应用启动完成后输出进程信息、JVM信息、端口信息等
 */
@Slf4j
@Component
public class StartupInfoListener implements ApplicationListener<ApplicationReadyEvent> {

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            Environment env = event.getApplicationContext().getEnvironment();
            RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            
            // 获取进程ID
            String processId = getProcessId(runtimeBean);
            
            // 获取JVM信息
            String jvmName = runtimeBean.getVmName();
            String jvmVersion = runtimeBean.getVmVersion();
            String javaVersion = System.getProperty("java.version");
            String javaHome = System.getProperty("java.home");
            
            // 获取内存信息
            long maxMemory = memoryBean.getHeapMemoryUsage().getMax() / 1024 / 1024; // MB
            long totalMemory = memoryBean.getHeapMemoryUsage().getInit() / 1024 / 1024; // MB
            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed() / 1024 / 1024; // MB
            
            // 获取应用信息
            String appName = env.getProperty("spring.application.name", "camp-app");
            String serverPort = env.getProperty("server.port", "8080");
            String activeProfile = String.join(",", env.getActiveProfiles().length > 0 
                ? env.getActiveProfiles() 
                : new String[]{"default"});
            
            // 获取数据库信息
            String datasourceUrl = env.getProperty("spring.datasource.url", "N/A");
            String datasourceUsername = env.getProperty("spring.datasource.username", "N/A");
            
            // 获取启动时间
            long startTime = runtimeBean.getStartTime();
            long uptime = runtimeBean.getUptime();
            Instant startInstant = Instant.ofEpochMilli(startTime);
            
            // 获取主机信息
            String hostName = InetAddress.getLocalHost().getHostName();
            String hostAddress = InetAddress.getLocalHost().getHostAddress();
            
            // 输出启动信息
            log.info("==========================================");
            log.info("应用启动完成！");
            log.info("==========================================");
            log.info("【应用信息】");
            log.info("  应用名称: {}", appName);
            log.info("  运行环境: {}", activeProfile);
            log.info("  服务端口: {}", serverPort);
            log.info("  访问地址: http://{}:{}", hostAddress, serverPort);
            log.info("  本地访问: http://localhost:{}", serverPort);
            log.info("");
            log.info("【进程信息】");
            log.info("  进程ID (PID): {}", processId);
            log.info("  主机名: {}", hostName);
            log.info("  IP地址: {}", hostAddress);
            log.info("  启动时间: {}", startInstant);
            log.info("  运行时长: {} 秒", uptime / 1000);
            log.info("");
            log.info("【JVM信息】");
            log.info("  JVM名称: {}", jvmName);
            log.info("  JVM版本: {}", jvmVersion);
            log.info("  Java版本: {}", javaVersion);
            log.info("  Java路径: {}", javaHome);
            log.info("");
            log.info("【内存信息】");
            log.info("  最大内存: {} MB", maxMemory);
            log.info("  初始内存: {} MB", totalMemory);
            log.info("  已用内存: {} MB", usedMemory);
            log.info("  可用内存: {} MB", (maxMemory - usedMemory));
            log.info("");
            log.info("【数据库信息】");
            log.info("  数据库URL: {}", maskPassword(datasourceUrl));
            log.info("  数据库用户: {}", datasourceUsername);
            log.info("");
            log.info("【快速操作】");
            log.info("  查看进程: ps -p {} | grep java", processId);
            log.info("  终止进程: kill {}", processId);
            log.info("  查看端口: lsof -i :{}", serverPort);
            log.info("==========================================");
            
        } catch (Exception e) {
            log.error("输出启动信息时发生错误", e);
        }
    }
    
    /**
     * 获取进程ID
     */
    private String getProcessId(RuntimeMXBean runtimeBean) {
        try {
            // 从运行时Bean获取进程ID（格式：pid@hostname）
            String name = runtimeBean.getName();
            if (name != null && name.contains("@")) {
                return name.split("@")[0];
            }
            // 备用方法：使用ProcessHandle（Java 9+）
            return String.valueOf(ProcessHandle.current().pid());
        } catch (Exception e) {
            return "N/A";
        }
    }
    
    /**
     * 隐藏数据库URL中的密码
     */
    private String maskPassword(String url) {
        if (url == null || url.equals("N/A")) {
            return url;
        }
        // 隐藏密码部分
        return url.replaceAll("password=[^&;]*", "password=***");
    }
}

