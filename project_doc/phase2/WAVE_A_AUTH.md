# Wave A — 密码与会话

前缀 `/api/v1`。响应体 `{ code, message, data, timestamp }`。HTTP 状态保持 200，业务码在 `code`。

开发态身份只在 `profile=h2`、`profile=dev` 或 `camp.dev-auth.enabled=true` 时生效：

- `Authorization: Bearer dev-ADMIN|dev-ACADEMIC|dev-TEACHER|dev-STUDENT`
- 或 `X-Dev-Role`，可选 `X-Dev-User-Id`、`X-Dev-Class-Ids`、`X-Dev-Grade-Ids`

其他 profile 把 `dev-*` 当成无效访问令牌（401）。

## 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/auth/login` | 密码登录。成功返回 `accessToken`、`refreshToken`、`isFirstLogin`、`userInfo`。访问令牌默认 2 小时，刷新令牌默认 7 天。 |
| POST | `/auth/refresh` | 用刷新令牌轮换。旧刷新令牌立即作废。 |
| POST | `/auth/logout` | 吊销刷新令牌。 |
| GET | `/auth/me` | 当前用户。 |
| PUT | `/auth/password` | 旧密码改新密码。成功后旧 access 失效，响应里的新 `accessToken` 可继续用。 |
| POST | `/auth/activate` | 首次设置密码。复杂度与改密相同。 |
| POST | `/auth/admin-reset-password` | ADMIN/ACADEMIC。临时口令满足复杂度，并强制下次登录改密。教务不能动管理员或教务。 |
| POST | `/auth/admin-unlock` | ADMIN/ACADEMIC。解除登录锁定。请求体 `{ "userId": 1 }`。 |

## 业务码

| code | 场景 |
| --- | --- |
| 401 | 未登录、密码错误、令牌无效或过期、刷新令牌作废 |
| 403 | 无权限，或 `isFirstLogin=true` 时调用了改密/登出/刷新以外的写接口 |
| 422 | 密码不符合复杂度，或与当前/历史密码相同 |
| 423 | 连续失败达到阈值，账号临时锁定 |

422 的 `message` 直接写规则，例如「密码长度不能少于8位」「密码须包含大写字母」「密码须包含小写字母」「密码须包含数字」「密码须包含特殊字符」「密码不能与用户名相同」「新密码不能与当前密码相同」。

## 配置

```
camp.auth.password.min-length=8
camp.auth.password.require-upper=true
camp.auth.password.require-lower=true
camp.auth.password.require-digit=true
camp.auth.password.require-special=true
camp.auth.password.reject-username=true
camp.auth.password.history-size=0
camp.auth.lock.max-failures=5
camp.auth.lock.lock-seconds=900
```

`history-size=0` 时只拒绝与当前密码相同。大于 0 时，本进程再记住这么多条旧哈希，重启后这部分记忆消失。`max-failures<=0` 关闭锁定。

演示口令 `Demo@123456` 满足默认复杂度。

## 审计

`sys_audit_logs.event_code` 仍只用首期的 1–6。

| 动作 | event_type | event_code | result |
| --- | --- | --- | --- |
| 登录失败、锁定后再试 | LOGIN_FAIL | 1 | 0 |
| 改密成功/失败、管理员重置 | PASSWORD_RESET | 2 | 1 或 0 |

密码明文不写进 `detail`。已知用户的 `target_user_id` 会填上。

## H2 冒烟

在仓库根目录：

```bash
./gradlew bootRun --args='--spring.profiles.active=h2'
```

另开终端。开发态与 JWT 在 h2 下都可用。

```bash
# 开发态
curl -s -H 'Authorization: Bearer dev-ADMIN' http://localhost:8080/api/v1/auth/me

# 密码登录
curl -s -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Demo@123456"}' \
  http://localhost:8080/api/v1/auth/login

# 把上一响应的 accessToken 填到下面
curl -s -H "Authorization: Bearer ACCESS" http://localhost:8080/api/v1/auth/me

# 复杂度失败，期望 code=422
curl -s -H "Authorization: Bearer ACCESS" -H 'Content-Type: application/json' \
  -X PUT -d '{"oldPassword":"Demo@123456","newPassword":"abcdef"}' \
  http://localhost:8080/api/v1/auth/password
```

非 h2/dev 的 JWT 路径用集成测试 `AuthWaveAIntegrationTest` 覆盖：同一套 H2 脚本，profile 名为 `jwt-it`，`dev-ADMIN` 应返回 401。手工启动时不要带 `h2` 或 `dev`，并显式把数据源指到 H2 脚本，例如：

```bash
./gradlew bootRun --args='--spring.profiles.active=jwt-it --spring.datasource.url=jdbc:h2:mem:campjwt;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE --spring.datasource.driver-class-name=org.h2.Driver --spring.datasource.username=sa --spring.datasource.password= --spring.jpa.database-platform=org.hibernate.dialect.H2Dialect --spring.jpa.hibernate.ddl-auto=none --spring.sql.init.mode=always --spring.sql.init.schema-locations=file:db/h2/schema.sql --spring.sql.init.data-locations=file:db/h2/seed.sql'
```

此时 `Bearer dev-ADMIN` 为 401，`admin` / `Demo@123456` 登录后应拿到 JWT。

自动化：

```bash
./gradlew test --tests com.camp.app.auth.AuthWaveAIntegrationTest --tests com.camp.app.auth.AuthDevProfileIntegrationTest --tests com.camp.app.security.PasswordPolicyTest --tests com.camp.app.security.LoginGuardTest --tests com.camp.app.security.JwtServiceTest
```
