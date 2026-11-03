package com.camp.app.security;

import com.camp.app.api.BizException;
import com.camp.app.config.AuthSecurityProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginGuardTest {

    @Test
    void locksOnThresholdAndUnlocks() {
        AuthSecurityProperties properties = new AuthSecurityProperties();
        properties.getLock().setMaxFailures(3);
        properties.getLock().setLockSeconds(600);
        LoginGuard guard = new LoginGuard(properties);

        guard.onFailure(7L);
        guard.onFailure(7L);
        BizException locked = assertThrows(BizException.class, () -> guard.onFailure(7L));
        assertEquals(423, locked.getCode());
        assertThrows(BizException.class, () -> guard.ensureAllowed(7L));

        guard.unlock(7L);
        assertDoesNotThrow(() -> guard.ensureAllowed(7L));
    }

    @Test
    void zeroThresholdDisablesLock() {
        AuthSecurityProperties properties = new AuthSecurityProperties();
        properties.getLock().setMaxFailures(0);
        LoginGuard guard = new LoginGuard(properties);
        assertDoesNotThrow(() -> {
            guard.onFailure(1L);
            guard.onFailure(1L);
            guard.ensureAllowed(1L);
        });
    }
}
