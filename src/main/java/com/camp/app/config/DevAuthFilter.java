package com.camp.app.config;

import com.camp.app.api.BaseResponse;
import com.camp.app.security.AuthContext;
import com.camp.app.security.AuthPrincipal;
import com.camp.app.security.DevAuthTokens;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RequiredArgsConstructor
public class DevAuthFilter extends OncePerRequestFilter {
    private final DevAuthProperties devAuthProperties;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!devAuthProperties.isEnabled() || !request.getRequestURI().startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }
        String authorization = request.getHeader("Authorization");
        String devRole = request.getHeader("X-Dev-Role");
        if (!DevAuthTokens.attempted(authorization, devRole)) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            AuthPrincipal principal = DevAuthTokens.resolve(
                    authorization,
                    devRole,
                    request.getHeader("X-Dev-User-Id"),
                    request.getHeader("X-Dev-Class-Ids"),
                    request.getHeader("X-Dev-Grade-Ids"));
            AuthContext.set(principal);
            filterChain.doFilter(request, response);
        } catch (IllegalArgumentException ex) {
            response.setStatus(200);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), BaseResponse.error(401, ex.getMessage()));
        } finally {
            AuthContext.clear();
        }
    }
}
