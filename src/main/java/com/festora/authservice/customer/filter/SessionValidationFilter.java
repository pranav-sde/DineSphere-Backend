package com.festora.authservice.customer.filter;

import com.festora.authservice.customer.context.SessionContext;
import com.festora.authservice.customer.dto.SessionData;
import com.festora.authservice.customer.validator.SessionStore;
import com.festora.authservice.customer.validator.SessionTokenValidator;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SessionValidationFilter extends OncePerRequestFilter {

    private final SessionTokenValidator tokenValidator;
    private final SessionStore sessionStore;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = request.getHeader("X-Session-Token");

        // Allow session start and admin auth endpoints without customer token
        String uri = request.getRequestURI();
        if (uri.startsWith("/session") || uri.startsWith("/auth/session") || 
            uri.startsWith("/login") || uri.startsWith("/auth/login") || 
            uri.startsWith("/health") || uri.startsWith("/auth/health") || 
            uri.startsWith("/register") || uri.startsWith("/auth/register") || 
            uri.startsWith("/admin") || uri.startsWith("/auth/admin") || 
            uri.startsWith("/owner") || uri.startsWith("/auth/owner")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (token == null || token.isBlank()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing session token");
            return;
        }

        try {
            Claims claims = tokenValidator.validate(token);

            String sessionId = claims.get("sid", String.class);

            SessionData session = sessionStore.get(sessionId);

            if (session == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Session expired");
                return;
            }

            SessionContext.set(session);

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid session");
        } finally {
            SessionContext.clear();
        }
    }
}

