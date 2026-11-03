package com.camp.app.security;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 改密后作废已发出的访问令牌。版本号放在 JWT 的 ver 声明里，服务端只记进程内计数。
 * 重启后计数归零，重启前签发的 ver=0 令牌仍然可用；刷新令牌本来也只活在本进程。
 */
@Component
public class SessionControl {
    private final ConcurrentHashMap<Long, Integer> versions = new ConcurrentHashMap<>();

    public int current(Long userId) {
        if (userId == null) {
            return 0;
        }
        return versions.getOrDefault(userId, 0);
    }

    public void bump(Long userId) {
        if (userId == null) {
            return;
        }
        versions.merge(userId, 1, Integer::sum);
    }

    public boolean accepts(Long userId, Object tokenVersion) {
        int presented = 0;
        if (tokenVersion instanceof Number number) {
            presented = number.intValue();
        }
        return presented == current(userId);
    }
}
