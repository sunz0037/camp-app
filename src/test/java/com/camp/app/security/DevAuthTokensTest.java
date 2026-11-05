package com.camp.app.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevAuthTokensTest {

    @Test
    void bearerRoleUsesDefaultUserAndPermissions() {
        AuthPrincipal principal = DevAuthTokens.resolve("Bearer dev-TEACHER", null, null, "1,2", null);
        assertTrue(principal.devAuth());
        assertEquals(3L, principal.userId());
        assertEquals("TEACHER", principal.roles().get(0));
        assertTrue(principal.hasPermission(Phase1Permissions.ATTENDANCE_RECORD));
        assertFalse(principal.hasPermission(Phase1Permissions.ORG_WRITE));
        assertEquals(2, principal.devClassIds().size());
        assertNull(principal.devGradeIds());
    }

    @Test
    void headerRoleOverridesBearerAndUser() {
        AuthPrincipal principal = DevAuthTokens.resolve("Bearer dev-ADMIN", "student", "9", "", "4");
        assertEquals("STUDENT", principal.roles().get(0));
        assertEquals(9L, principal.userId());
        assertTrue(principal.devClassIds().isEmpty());
        assertEquals(4L, principal.devGradeIds().get(0));
    }

    @Test
    void attemptedOnlyWhenDevCredentialPresent() {
        assertFalse(DevAuthTokens.attempted(null, null));
        assertFalse(DevAuthTokens.attempted("Bearer eyJabc", null));
        assertTrue(DevAuthTokens.attempted("Bearer dev-ADMIN", null));
        assertTrue(DevAuthTokens.attempted(null, "ACADEMIC"));
    }
}
