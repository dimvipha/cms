package com.vipha.cms.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class AuthUtils {

    private AuthUtils() {}

    public static String extractUserId() {
        Authentication auth = getAuth();

        if (auth == null || auth instanceof AnonymousAuthenticationToken) {
            throw new InsufficientAuthenticationException("Authentication required");
        }

        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            throw new InsufficientAuthenticationException("JWT authentication required");
        }

        return jwtAuth.getToken().getSubject();
    }

    public static String extractJwt() {
        Authentication auth = getAuth();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return jwt.getTokenValue();
        }
        throw new InsufficientAuthenticationException("Authentication required");
    }

    public static Authentication getAuth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

}