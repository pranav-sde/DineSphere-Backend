package com.festora.authservice.filter;

import com.festora.authservice.security.JwtValidator;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtValidator jwtValidator;

    public JwtAuthenticationFilter(JwtValidator jwtValidator) {
        this.jwtValidator = jwtValidator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response, FilterChain filterChain) throws java.io.IOException, jakarta.servlet.ServletException {

        String path = request.getRequestURI();
        if (path.startsWith("/session") || path.startsWith("/login") || path.startsWith("/register") || path.startsWith("/health") || path.startsWith("/auth/session") || path.startsWith("/auth/login") || path.startsWith("/auth/register") || path.startsWith("/auth/health")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtValidator.validate(token).getBody();

            String userId = claims.getSubject();
            String role = claims.get("role", String.class);

            var authentication = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    List.of(() -> "ROLE_" + role)
            );

            request.setAttribute(
                    "restaurantId",
                    claims.get("restaurantId", Long.class)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }
}