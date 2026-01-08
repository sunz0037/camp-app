# 数据库连接状态报告

## 测试结果

### 网络连通性
- ✅ **服务器可达**：150.158.115.149 可以ping通
- ❌ **端口不通**：3306端口连接超时

### 连接测试
- **测试时间**：2024-12-24
- **测试方法**：nc命令测试端口连接
- **结果**：`Connection timed out`

## 问题分析

### 根本原因
MySQL服务器的3306端口无法从外部访问，可能的原因：

1. **防火墙阻止**：服务器防火墙未开放3306端口
2. **MySQL配置**：MySQL只监听127.0.0.1，未监听公网IP
3. **安全组规则**：云服务器的安全组未开放3306端口
4. **MySQL服务未启动**：MySQL服务可能未运行

## 解决方案

### 方案1：使用内网地址（最推荐）

如果应用服务器和MySQL服务器在同一内网，修改配置使用内网地址：

```properties
# 修改 application.properties
spring.datasource.url=jdbc:mysql://10.0.0.3:3306/camp?...
```

**优点**：
- 连接更稳定
- 延迟更低
- 更安全

### 方案2：在MySQL服务器上开放端口

#### 2.1 检查防火墙（CentOS/RHEL）
```bash
# 查看防火墙状态
firewall-cmd --list-all

# 开放3306端口
firewall-cmd --permanent --add-port=3306/tcp
firewall-cmd --reload

# 验证
firewall-cmd --list-ports
```

#### 2.2 检查防火墙（Ubuntu/Debian）
```bash
# 查看防火墙状态
ufw status

# 开放3306端口
ufw allow 3306/tcp

# 验证
ufw status
```

#### 2.3 检查云服务器安全组
- 登录云服务器控制台
- 找到安全组规则
- 添加入站规则：允许TCP 3306端口

### 方案3：修改MySQL配置

#### 3.1 检查MySQL监听地址
```bash
# 在MySQL服务器上执行
netstat -tlnp | grep 3306
# 或
ss -tlnp | grep 3306
```

**期望结果**：应该看到 `0.0.0.0:3306` 或 `:::3306`

**如果只看到 `127.0.0.1:3306`**，需要修改配置：

#### 3.2 修改MySQL配置文件
```bash
# 编辑MySQL配置文件
# CentOS/RHEL: /etc/my.cnf 或 /etc/mysql/my.cnf
# Ubuntu/Debian: /etc/mysql/mysql.conf.d/mysqld.cnf

[mysqld]
bind-address = 0.0.0.0  # 允许所有IP连接
# 或注释掉 bind-address 行

# 重启MySQL服务
systemctl restart mysql
# 或
systemctl restart mysqld
```

### 方案4：检查用户权限

确保campuser用户有远程连接权限：

```sql
-- 连接到MySQL（使用root用户）
mysql -u root -p

-- 检查用户
SELECT user, host FROM mysql.user WHERE user = 'campuser';

-- 如果用户不存在或host不是%，创建/修改用户
CREATE USER IF NOT EXISTS 'campuser'@'%' IDENTIFIED BY 'camp123456';
GRANT ALL PRIVILEGES ON camp.* TO 'campuser'@'%';
FLUSH PRIVILEGES;

-- 验证权限
SHOW GRANTS FOR 'campuser'@'%';
```

### 方案5：使用SSH隧道（临时方案）

如果有SSH访问权限，可以建立隧道：

```bash
# 建立SSH隧道
ssh -L 3307:localhost:3306 user@150.158.115.149

# 然后修改应用配置连接本地3307端口
spring.datasource.url=jdbc:mysql://localhost:3307/camp?...
```

## 验证步骤

### 1. 测试端口是否开放
```bash
# 从应用服务器测试
nc -zv -w 5 150.158.115.149 3306

# 如果成功，应该看到：
# Connection to 150.158.115.149 port 3306 [tcp/mysql] succeeded!
```

### 2. 测试MySQL连接
```bash
# 如果端口通了，测试MySQL连接
mysql -h 150.158.115.149 -P 3306 -u campuser -pcamp123456 -e "SELECT 1;"
```

### 3. 启动应用测试
```bash
cd /Users/sunz/AIProject/camp-app
./gradlew bootRun
```

查看启动日志，确认数据库连接成功。

## 当前配置

- **数据库地址**：150.158.115.149:3306
- **数据库名**：camp
- **用户名**：campuser
- **密码**：camp123456
- **连接超时**：30秒
- **重连次数**：3次

## 建议

1. **优先使用内网地址**：如果可能，使用 `10.0.0.3:3306`
2. **检查服务器配置**：确认MySQL服务器已正确配置
3. **联系服务器管理员**：如果需要开放端口或修改配置

## 下一步

1. 确认MySQL服务器的网络配置
2. 开放3306端口或使用内网地址
3. 重新测试连接
4. 启动应用验证

