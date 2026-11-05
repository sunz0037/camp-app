package com.camp.app.security;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 首期库没有 refresh token 表。吊销状态只放在进程内，密码登录不是联调主路径。
 */
@Component
public class RefreshTokenStore {
    public record Token(Long userId, LocalDateTime expiresAt, boolean revoked) {
    }

    private final ConcurrentHashMap<String, Token> tokens = new ConcurrentHashMap<>();

    public void save(String jti, Long userId, LocalDateTime expiresAt) {
        tokens.put(jti, new Token(userId, expiresAt, false));
    }

    public Optional<Token> find(String jti) {
        return Optional.ofNullable(tokens.get(jti));
    }

    public void revoke(String jti) {
        tokens.computeIfPresent(jti, (key, token) -> new Token(token.userId(), token.expiresAt(), true));
    }
}
