package com.camp.app.security;

import com.camp.app.config.AuthSecurityProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 可选的历史口令。默认 history-size=0，只靠当前口令比对。
 * 进程内保存，和刷新令牌一样不落首期表。
 */
@Component
public class PasswordHistory {
    private final int limit;
    private final ConcurrentHashMap<Long, Deque<String>> older = new ConcurrentHashMap<>();

    public PasswordHistory(AuthSecurityProperties properties) {
        this.limit = Math.max(0, properties.getPassword().getHistorySize());
    }

    public List<String> olderHashes(Long userId) {
        if (limit <= 0 || userId == null) {
            return List.of();
        }
        Deque<String> deque = older.get(userId);
        if (deque == null) {
            return List.of();
        }
        synchronized (deque) {
            return List.copyOf(deque);
        }
    }

    public void remember(Long userId, String encoded) {
        if (limit <= 0 || userId == null || encoded == null || encoded.isBlank()) {
            return;
        }
        Deque<String> deque = older.computeIfAbsent(userId, id -> new ArrayDeque<>());
        synchronized (deque) {
            deque.addFirst(encoded);
            while (deque.size() > limit) {
                deque.removeLast();
            }
        }
    }
}
