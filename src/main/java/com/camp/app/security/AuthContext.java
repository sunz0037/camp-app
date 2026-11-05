package com.camp.app.security;

public final class AuthContext {
    private static final ThreadLocal<AuthPrincipal> CURRENT = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(AuthPrincipal principal) {
        CURRENT.set(principal);
    }

    public static AuthPrincipal get() {
        return CURRENT.get();
    }

    public static Long userId() {
        AuthPrincipal principal = CURRENT.get();
        return principal == null ? null : principal.userId();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
