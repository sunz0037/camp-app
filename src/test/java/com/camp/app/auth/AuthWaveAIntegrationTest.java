package com.camp.app.auth;

import com.camp.app.entity.AuditLog;
import com.camp.app.repository.AuditLogRepository;
import com.camp.app.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 非 h2/dev profile，数据源仍是首期 H2 脚本。开发态令牌必须被拒绝，密码登录走 JWT。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:campjwt;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.show-sql=false",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=file:db/h2/schema.sql",
        "spring.sql.init.data-locations=file:db/h2/seed.sql",
        "spring.sql.init.encoding=UTF-8",
        "camp.dev-auth.enabled=false",
        "camp.auth.lock.max-failures=3",
        "camp.auth.lock.lock-seconds=600"
})
@ActiveProfiles("jwt-it")
class AuthWaveAIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void jwtLoginPasswordPolicyAndLock() {
        JsonNode anonymous = get("/api/v1/auth/me", null);
        assertEquals(401, anonymous.path("code").asInt());

        JsonNode dev = get("/api/v1/auth/me", "dev-ADMIN");
        assertEquals(401, dev.path("code").asInt());
        assertTrue(dev.path("message").asText().contains("访问令牌"));

        JsonNode adminLogin = post("/api/v1/auth/login", Map.of("username", "admin", "password", "Demo@123456"), null);
        assertEquals(200, adminLogin.path("code").asInt());
        String adminAccess = adminLogin.path("data").path("accessToken").asText();
        String adminRefresh = adminLogin.path("data").path("refreshToken").asText();
        assertFalse(adminAccess.isBlank());
        assertEquals("admin", get("/api/v1/auth/me", adminAccess).path("data").path("username").asText());

        JsonNode refreshed = post("/api/v1/auth/refresh", Map.of("refreshToken", adminRefresh), null);
        assertEquals(200, refreshed.path("code").asInt());
        String rotated = refreshed.path("data").path("accessToken").asText();
        String rotatedRefresh = refreshed.path("data").path("refreshToken").asText();
        assertNotEquals(adminRefresh, rotatedRefresh);
        assertEquals(401, post("/api/v1/auth/refresh", Map.of("refreshToken", adminRefresh), null).path("code").asInt());
        assertEquals(200, get("/api/v1/auth/me", rotated).path("code").asInt());
        JsonNode loggedOut = post("/api/v1/auth/logout", Map.of("refreshToken", rotatedRefresh), rotated);
        assertEquals(200, loggedOut.path("code").asInt());
        assertEquals(401, post("/api/v1/auth/refresh", Map.of("refreshToken", rotatedRefresh), null).path("code").asInt());

        JsonNode studentLogin = post("/api/v1/auth/login",
                Map.of("username", "student01", "password", "Demo@123456"), null);
        String studentAccess = studentLogin.path("data").path("accessToken").asText();
        JsonNode weak = put("/api/v1/auth/password",
                Map.of("oldPassword", "Demo@123456", "newPassword", "abcdef"), studentAccess);
        assertEquals(422, weak.path("code").asInt());
        assertTrue(weak.path("message").asText().contains("密码"));
        JsonNode same = put("/api/v1/auth/password",
                Map.of("oldPassword", "Demo@123456", "newPassword", "Demo@123456"), studentAccess);
        assertEquals(422, same.path("code").asInt());

        JsonNode changed = put("/api/v1/auth/password",
                Map.of("oldPassword", "Demo@123456", "newPassword", "Newpass@123"), studentAccess);
        assertEquals(200, changed.path("code").asInt());
        String fresh = changed.path("data").path("accessToken").asText();
        assertEquals(401, get("/api/v1/auth/me", studentAccess).path("code").asInt());
        assertEquals(200, get("/api/v1/auth/me", fresh).path("code").asInt());
        assertEquals(401, post("/api/v1/auth/login",
                Map.of("username", "student01", "password", "Demo@123456"), null).path("code").asInt());
        assertEquals(200, post("/api/v1/auth/login",
                Map.of("username", "student01", "password", "Newpass@123"), null).path("code").asInt());

        Long teacherId = userRepository.findByUsername("teacher01").orElseThrow().getId();
        assertEquals(401, post("/api/v1/auth/login", Map.of("username", "teacher01", "password", "bad-1"), null).path("code").asInt());
        assertEquals(401, post("/api/v1/auth/login", Map.of("username", "teacher01", "password", "bad-2"), null).path("code").asInt());
        JsonNode locked = post("/api/v1/auth/login", Map.of("username", "teacher01", "password", "bad-3"), null);
        assertEquals(423, locked.path("code").asInt());
        assertEquals(423, post("/api/v1/auth/login",
                Map.of("username", "teacher01", "password", "Demo@123456"), null).path("code").asInt());

        JsonNode unlocked = post("/api/v1/auth/admin-unlock", Map.of("userId", teacherId), rotated);
        assertEquals(200, unlocked.path("code").asInt());
        assertEquals(200, post("/api/v1/auth/login",
                Map.of("username", "teacher01", "password", "Demo@123456"), null).path("code").asInt());

        boolean loginFail = auditLogRepository.findAll().stream().anyMatch(this::isLoginFail);
        boolean passwordReset = auditLogRepository.findAll().stream().anyMatch(this::isPasswordReset);
        assertTrue(loginFail);
        assertTrue(passwordReset);
    }

    private boolean isLoginFail(AuditLog row) {
        return "LOGIN_FAIL".equals(row.getEventType())
                && Integer.valueOf(1).equals(row.getEventCode())
                && Integer.valueOf(0).equals(row.getResult())
                && row.getTargetUserId() != null;
    }

    private boolean isPasswordReset(AuditLog row) {
        return "PASSWORD_RESET".equals(row.getEventType())
                && Integer.valueOf(2).equals(row.getEventCode())
                && Integer.valueOf(1).equals(row.getResult());
    }

    private JsonNode get(String path, String bearer) {
        return exchange(path, HttpMethod.GET, null, bearer);
    }

    private JsonNode post(String path, Object body, String bearer) {
        return exchange(path, HttpMethod.POST, body, bearer);
    }

    private JsonNode put(String path, Object body, String bearer) {
        return exchange(path, HttpMethod.PUT, body, bearer);
    }

    private JsonNode exchange(String path, HttpMethod method, Object body, String bearer) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (bearer != null) {
            headers.setBearerAuth(bearer);
        }
        ResponseEntity<JsonNode> response = rest.exchange(path, method, new HttpEntity<>(body, headers), JsonNode.class);
        return response.getBody();
    }
}
