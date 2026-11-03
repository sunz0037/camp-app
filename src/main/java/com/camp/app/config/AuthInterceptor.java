package com.camp.app.config;

import com.camp.app.api.BaseResponse;
import com.camp.app.security.AuthContext;
import com.camp.app.security.AuthPrincipal;
import com.camp.app.security.JwtService;
import com.camp.app.security.RequirePermission;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final DevAuthProperties devAuthProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        boolean activate = "POST".equalsIgnoreCase(request.getMethod()) && path.endsWith("/api/v1/auth/activate");
        if (isPublic(request) || activate) {
            if (AuthContext.get() == null) {
                bindIfPresent(request);
            }
            return true;
        }
        AuthPrincipal principal = AuthContext.get();
        if (principal == null) {
            principal = bindIfPresent(request);
        }
        if (principal == null) {
            String message = devAuthProperties.isEnabled()
                    ? "未登录。开发态请使用 Authorization: Bearer dev-ADMIN|dev-ACADEMIC|dev-TEACHER|dev-STUDENT，或请求头 X-Dev-Role"
                    : "未登录";
            write(response, 401, message);
            return false;
        }
        if (principal.mustChangePassword() && isWrite(request) && !allowedWhileFirstLogin(path)) {
            write(response, 403, "请先完成首次登录改密");
            return false;
        }
        if (handler instanceof HandlerMethod method) {
            RequirePermission permission = method.getMethodAnnotation(RequirePermission.class);
            if (permission != null && !principal.hasPermission(permission.value())) {
                write(response, 403, "无权限");
                return false;
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }

    private boolean isPublic(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI();
        return path.endsWith("/api/v1/auth/login") || path.endsWith("/api/v1/auth/refresh");
    }

    private boolean isWrite(HttpServletRequest request) {
        String method = request.getMethod();
        return "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method);
    }

    private boolean allowedWhileFirstLogin(String path) {
        return path.contains("/api/v1/auth/activate")
                || path.contains("/api/v1/auth/logout")
                || path.contains("/api/v1/auth/password")
                || path.contains("/api/v1/auth/refresh");
    }

    private AuthPrincipal bindIfPresent(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        String token = header.substring(7).trim();
        if (token.isEmpty()) {
            return null;
        }
        try {
            Claims claims = jwtService.parse(token);
            AuthPrincipal principal = jwtService.toPrincipal(claims);
            AuthContext.set(principal);
            return principal;
        } catch (Exception ex) {
            return null;
        }
    }

    private void write(HttpServletResponse response, int code, String message) throws Exception {
        response.setStatus(200);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), BaseResponse.error(code, message));
    }
}
