package com.camp.app.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class JwtService {
    private final SecretKey key;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;

    public JwtService(
            @Value("${camp.jwt.secret:camp-phase1-dev-secret-please-change}") String secret,
            @Value("${camp.jwt.access-ttl-seconds:7200}") long accessTtlSeconds,
            @Value("${camp.jwt.refresh-ttl-seconds:604800}") long refreshTtlSeconds) {
        this.key = Keys.hmacShaKeyFor(normalizeSecret(secret));
        this.accessTtlSeconds = accessTtlSeconds;
        this.refreshTtlSeconds = refreshTtlSeconds;
    }

    public String createAccessToken(Long userId, List<String> roles, List<String> permissions, boolean mustChangePassword) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("typ", "access")
                .claim("roles", roles)
                .claim("perms", permissions)
                .claim("mcp", mustChangePassword)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTtlSeconds)))
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(Long userId, String jti) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .id(jti)
                .claim("typ", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshTtlSeconds)))
                .signWith(key)
                .compact();
    }

    public String newJti() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getRefreshTtlSeconds() {
        return refreshTtlSeconds;
    }

    @SuppressWarnings("unchecked")
    public AuthPrincipal toPrincipal(Claims claims) {
        if (!"access".equals(claims.get("typ", String.class))) {
            throw new IllegalArgumentException("不是访问令牌");
        }
        Long userId = Long.valueOf(claims.getSubject());
        List<String> roles = claims.get("roles", List.class);
        List<String> perms = claims.get("perms", List.class);
        Boolean mcp = claims.get("mcp", Boolean.class);
        return new AuthPrincipal(
                userId,
                roles == null ? List.of() : roles,
                perms == null ? List.of() : perms,
                Boolean.TRUE.equals(mcp));
    }

    private static byte[] normalizeSecret(String secret) {
        byte[] raw = (secret == null ? "" : secret).getBytes(StandardCharsets.UTF_8);
        if (raw.length >= 32) {
            return raw;
        }
        try {
            return MessageDigest.getInstance("SHA-256").digest(raw);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
