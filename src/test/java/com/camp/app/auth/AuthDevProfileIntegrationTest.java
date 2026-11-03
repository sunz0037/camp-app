package com.camp.app.auth;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:camph2dev;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.show-sql=false"
})
@ActiveProfiles("h2")
class AuthDevProfileIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private UserRepository userRepository;

    @Test
    void h2KeepsDevHeadersAndJwtLogin() {
        JsonNode dev = exchange("/api/v1/auth/me", HttpMethod.GET, null, "dev-ADMIN", null);
        assertEquals(200, dev.path("code").asInt());
        assertEquals("ADMIN", dev.path("data").path("roles").get(0).asText());

        HttpHeaders roleHeader = new HttpHeaders();
        roleHeader.set("X-Dev-Role", "TEACHER");
        ResponseEntity<JsonNode> byHeader = rest.exchange("/api/v1/auth/me", HttpMethod.GET,
                new HttpEntity<>(roleHeader), JsonNode.class);
        assertEquals(200, byHeader.getBody().path("code").asInt());
        assertEquals("TEACHER", byHeader.getBody().path("data").path("roles").get(0).asText());

        JsonNode login = exchange("/api/v1/auth/login", HttpMethod.POST,
                Map.of("username", "admin", "password", "Demo@123456"), null, null);
        assertEquals(200, login.path("code").asInt());
        assertFalse(login.path("data").path("accessToken").asText().isBlank());
        String adminToken = login.path("data").path("accessToken").asText();

        Long adminId = userRepository.findByUsername("admin").orElseThrow().getId();
        JsonNode denied = exchange("/api/v1/auth/admin-unlock", HttpMethod.POST,
                Map.of("userId", adminId), null, "ACADEMIC");
        assertEquals(403, denied.path("code").asInt());

        Long studentId = userRepository.findByUsername("student01").orElseThrow().getId();
        JsonNode reset = exchange("/api/v1/auth/admin-reset-password", HttpMethod.POST,
                Map.of("userId", studentId), adminToken, null);
        assertEquals(200, reset.path("code").asInt());
        String temporary = reset.path("data").path("temporaryPassword").asText();
        assertTrue(temporary.length() >= 8);

        JsonNode forced = exchange("/api/v1/auth/login", HttpMethod.POST,
                Map.of("username", "student01", "password", temporary), null, null);
        assertEquals(200, forced.path("code").asInt());
        assertTrue(forced.path("data").path("isFirstLogin").asBoolean());
    }

    private JsonNode exchange(String path, HttpMethod method, Object body, String bearer, String devRole) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (bearer != null) {
            headers.setBearerAuth(bearer);
        }
        if (devRole != null) {
            headers.set("X-Dev-Role", devRole);
        }
        ResponseEntity<JsonNode> response = rest.exchange(path, method, new HttpEntity<>(body, headers), JsonNode.class);
        return response.getBody();
    }
}
