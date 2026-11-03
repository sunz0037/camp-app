package com.camp.app.security;

import com.camp.app.config.DevAuthProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    @Test
    void accessTokenCarriesVersionAndRefreshIsNotAccess() {
        JwtService jwt = new JwtService("test-secret-value-32-bytes-minimum!!", 3600, 86400);
        String access = jwt.createAccessToken(4L, List.of("STUDENT"), List.of(), false, 2);
        Claims claims = jwt.parse(access);
        assertEquals("access", claims.get("typ", String.class));
        assertEquals(2, ((Number) claims.get("ver")).intValue());
        assertEquals(4L, jwt.toPrincipal(claims).userId());

        String refresh = jwt.createRefreshToken(4L, jwt.newJti());
        assertThrows(IllegalArgumentException.class, () -> jwt.toPrincipal(jwt.parse(refresh)));
    }

    @Test
    void sessionVersionRejectsTokenIssuedBeforePasswordChange() {
        SessionControl sessions = new SessionControl();
        assertTrue(sessions.accepts(4L, 0));
        assertTrue(sessions.accepts(4L, null));
        sessions.bump(4L);
        assertFalse(sessions.accepts(4L, 0));
        assertTrue(sessions.accepts(4L, 1));
    }

    @Test
    void devAuthFollowsProfile() {
        StandardEnvironment h2 = new StandardEnvironment();
        h2.setActiveProfiles("h2");
        assertTrue(new DevAuthProperties(false, h2).isEnabled());

        StandardEnvironment dev = new StandardEnvironment();
        dev.setActiveProfiles("dev");
        assertTrue(new DevAuthProperties(false, dev).isEnabled());

        StandardEnvironment prod = new StandardEnvironment();
        prod.setActiveProfiles("prod");
        assertFalse(new DevAuthProperties(false, prod).isEnabled());
        assertTrue(new DevAuthProperties(true, prod).isEnabled());
    }
}
