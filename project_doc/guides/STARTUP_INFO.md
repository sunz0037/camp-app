# 应用启动信息说明

## 功能说明

应用启动完成后，会自动在日志中输出详细的启动信息，包括：
- 进程ID (PID)
- JVM信息
- 内存信息
- 数据库连接信息
- 访问地址
- 快速操作命令

## 启动日志示例

应用启动完成后，会看到类似以下的日志输出：

```
==========================================
应用启动完成！
==========================================
【应用信息】
  应用名称: camp-app
  运行环境: default
  服务端口: 8080
  访问地址: http://192.168.1.100:8080
  本地访问: http://localhost:8080

【进程信息】
  进程ID (PID): 87481
  主机名: MacBook-Pro.local
  IP地址: 192.168.1.100
  启动时间: 2024-12-24T10:43:00Z
  运行时长: 5 秒

【JVM信息】
  JVM名称: OpenJDK 64-Bit Server VM
  JVM版本: OpenJDK 64-Bit Server VM (21.0.1+12-LTS, mixed mode, sharing)
  Java版本: 21.0.1
  Java路径: /Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home

【内存信息】
  最大内存: 2048 MB
  初始内存: 256 MB
  已用内存: 180 MB
  可用内存: 1868 MB

【数据库信息】
  数据库URL: jdbc:mysql://150.158.115.149:3306/camp?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&autoReconnect=true&failOverReadOnly=false&maxReconnects=3&initialTimeout=2&connectTimeout=30000&socketTimeout=60000&tcpKeepAlive=true&tcpNoDelay=true&password=***
  数据库用户: campuser

【快速操作】
  查看进程: ps -p 87481 | grep java
  终止进程: kill 87481
  查看端口: lsof -i :8080
==========================================
```

## 输出信息说明

### 应用信息
- **应用名称**：从 `spring.application.name` 配置读取
- **运行环境**：当前激活的Spring Profile
- **服务端口**：应用监听的端口号
- **访问地址**：本机IP地址和端口
- **本地访问**：localhost访问地址

### 进程信息
- **进程ID (PID)**：Java进程的进程号，可用于进程管理
- **主机名**：运行应用的主机名称
- **IP地址**：本机IP地址
- **启动时间**：应用启动的时间戳
- **运行时长**：应用已运行的秒数

### JVM信息
- **JVM名称**：Java虚拟机名称
- **JVM版本**：JVM详细版本信息
- **Java版本**：Java运行时版本
- **Java路径**：Java安装路径

### 内存信息
- **最大内存**：JVM最大堆内存（MB）
- **初始内存**：JVM初始堆内存（MB）
- **已用内存**：当前已使用的堆内存（MB）
- **可用内存**：剩余可用堆内存（MB）

### 数据库信息
- **数据库URL**：数据库连接URL（密码已隐藏）
- **数据库用户**：数据库用户名

### 快速操作
提供常用的进程管理命令，方便快速操作：
- 查看进程状态
- 终止进程
- 查看端口占用

## 使用方法

### 启动应用
```bash
cd /Users/sunz/AIProject/camp-app
./gradlew bootRun
```

### 查看启动日志
启动日志会输出到控制台，包含完整的启动信息。

### 使用进程ID
启动后，可以使用输出的进程ID进行进程管理：

```bash
# 查看进程详细信息
ps -p <PID> -f

# 终止进程
kill <PID>

# 强制终止进程
kill -9 <PID>

# 查看进程占用的端口
lsof -i -P | grep <PID>
```

## 实现原理

启动信息监听器 `StartupInfoListener` 实现了 `ApplicationListener<ApplicationReadyEvent>` 接口，在应用完全启动后（所有Bean初始化完成）触发，输出详细的启动信息。

## 注意事项

1. **进程ID获取**：使用 `RuntimeMXBean.getName()` 或 `ProcessHandle.current().pid()` 获取进程ID
2. **密码隐藏**：数据库URL中的密码会被自动隐藏为 `***`
3. **内存单位**：内存信息统一使用MB（兆字节）作为单位
4. **启动时间**：显示的是JVM启动时间，不是应用完全就绪时间

## 自定义配置

如果需要修改输出内容，可以编辑 `StartupInfoListener.java` 文件，调整输出的信息项。

