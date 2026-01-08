# CORS跨域配置说明

## 问题描述

前端（localhost:3000）访问后端API（localhost:8080）时，浏览器发送OPTIONS预检请求返回403 Forbidden错误。

## 解决方案

已添加CORS（跨域资源共享）配置，允许前端访问后端API。

## 配置内容

### CORS配置类

**文件位置**: `src/main/java/com/camp/app/config/CorsConfig.java`

**配置项**：
- **允许的源**: `http://localhost:3000`, `http://127.0.0.1:3000`
- **允许的方法**: GET, POST, PUT, DELETE, OPTIONS, PATCH
- **允许的请求头**: 所有（*）
- **允许凭证**: 是（allowCredentials: true）
- **预检缓存**: 3600秒

### 配置方式

使用两种方式确保CORS配置生效：

1. **WebMvcConfigurer** - 通过`addCorsMappings`配置
2. **CorsFilter** - 通过过滤器配置（备用方案）

## 验证结果

### OPTIONS预检请求

```bash
curl -X OPTIONS http://localhost:8080/api/auth/login \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: Content-Type"
```

**响应头**：
```
HTTP/1.1 200
Access-Control-Allow-Origin: http://localhost:3000
Access-Control-Allow-Methods: POST
Access-Control-Allow-Headers: Content-Type
Access-Control-Allow-Credentials: true
Access-Control-Max-Age: 3600
```

### 实际请求

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -H "Origin: http://localhost:3000" \
  -d '{"identityType":1,"identifier":"20240001","realName":"张三","password":""}'
```

**响应头**：
```
Access-Control-Allow-Origin: http://localhost:3000
Access-Control-Allow-Credentials: true
```

## 配置说明

### Spring Boot 3.x注意事项

在Spring Boot 3.x中：
- 使用 `allowedOriginPatterns` 而不是 `allowedOrigins`
- 如果设置了 `allowCredentials(true)`，必须明确指定源，不能使用 `*`

### 生产环境配置

生产环境需要修改允许的源：

```java
.allowedOriginPatterns(
    "http://localhost:3000",
    "https://your-production-domain.com"
)
```

## 常见问题

### 1. 仍然返回403

**原因**：
- 服务未重启，配置未生效
- 浏览器缓存了旧的CORS响应

**解决方案**：
1. 重启后端服务
2. 清除浏览器缓存
3. 使用无痕模式测试

### 2. OPTIONS请求成功，但POST请求失败

**原因**：
- 可能是其他配置问题（如Spring Security）

**解决方案**：
- 检查是否有其他安全配置拦截请求
- 查看后端日志

### 3. 生产环境CORS问题

**原因**：
- 生产环境域名不同

**解决方案**：
- 修改CORS配置，添加生产环境域名
- 或使用环境变量配置

## 测试步骤

1. **重启后端服务**
   ```bash
   cd camp-app
   ./gradlew bootRun
   ```

2. **测试OPTIONS请求**
   ```bash
   curl -X OPTIONS http://localhost:8080/api/auth/login \
     -H "Origin: http://localhost:3000" \
     -H "Access-Control-Request-Method: POST" \
     -v
   ```

3. **测试实际请求**
   ```bash
   curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -H "Origin: http://localhost:3000" \
     -d '{"identityType":1,"identifier":"20240001","realName":"张三","password":""}'
   ```

4. **前端测试**
   - 打开浏览器访问 http://localhost:3000
   - 尝试登录
   - 查看浏览器控制台，应该没有CORS错误

## 相关文件

- `src/main/java/com/camp/app/config/CorsConfig.java` - CORS配置类
- `src/main/java/com/camp/app/controller/AuthController.java` - 登录控制器

## 总结

✅ CORS配置已添加
✅ 允许前端（localhost:3000）访问后端API
✅ OPTIONS预检请求正常响应
✅ 支持所有必要的HTTP方法和请求头

现在前端可以正常访问后端API，不会再出现403错误。

