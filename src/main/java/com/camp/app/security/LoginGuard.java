package com.camp.app.security;

import com.camp.app.api.BizException;
import com.camp.app.config.AuthSecurityProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 连续登录失败锁定。计数在进程内，审计仍写 sys_audit_logs 的 LOGIN_FAIL。
 * 解锁：到期、登录成功、改密/重置，或管理员调用解锁接口。
 */
@Component
public class LoginGuard {
    private static final class State {
        private int failures;
        private Instant lockedUntil;
    }

    private final int maxFailures;
    private final Duration lockFor;
    private final ConcurrentHashMap<Long, State> states = new ConcurrentHashMap<>();

    public LoginGuard(AuthSecurityProperties properties) {
        this.maxFailures = properties.getLock().getMaxFailures();
        int seconds = properties.getLock().getLockSeconds();
        this.lockFor = Duration.ofSeconds(Math.max(seconds, 1));
    }

    public void ensureAllowed(Long userId) {
        if (!enabled() || userId == null) {
            return;
        }
        State state = states.get(userId);
        if (state == null) {
            return;
        }
        synchronized (state) {
            if (isLocked(state)) {
                throw locked(state);
            }
        }
    }

    public void onFailure(Long userId) {
        if (!enabled() || userId == null) {
            return;
        }
        State state = states.computeIfAbsent(userId, id -> new State());
        synchronized (state) {
            if (isLocked(state)) {
                throw locked(state);
            }
            if (state.lockedUntil != null) {
                state.lockedUntil = null;
                state.failures = 0;
            }
            state.failures++;
            if (state.failures >= maxFailures) {
                state.lockedUntil = Instant.now().plus(lockFor);
                throw locked(state);
            }
        }
    }

    public void unlock(Long userId) {
        if (userId == null) {
            return;
        }
        states.remove(userId);
    }

    private boolean enabled() {
        return maxFailures > 0;
    }

    private boolean isLocked(State state) {
        if (state.lockedUntil == null) {
            return false;
        }
        if (Instant.now().isBefore(state.lockedUntil)) {
            return true;
        }
        state.lockedUntil = null;
        state.failures = 0;
        return false;
    }

    private BizException locked(State state) {
        long seconds = Math.max(1, Duration.between(Instant.now(), state.lockedUntil).toSeconds());
        return BizException.locked("登录失败次数过多，账号已临时锁定，请 " + seconds + " 秒后再试或联系管理员解锁");
    }
}
