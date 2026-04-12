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
        
        // Skip for public endpoints (already handled by SecurityConfig, but extra layer)
        if (isPublicPath(path)) {
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

            // 1. Extract Claims
            String sid = claims.get("sid", String.class);
            if (sid == null) sid = claims.getSubject(); // for admin tokens
            
            Long restaurantId = claims.get("restaurantId", Long.class);
            Integer tableNumber = claims.get("tableNumber", Integer.class);
            String deviceId = claims.get("deviceId", String.class);
            String role = claims.get("role", String.class);

            // 2. Set Spring Security Authentication
            var authentication = new UsernamePasswordAuthenticationToken(
                    sid,
                    null,
                    List.of(() -> "ROLE_" + (role != null ? role : "CUSTOMER"))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 3. Inject into Headers via Wrapper (for @RequestHeader compatibility)
            HeaderMapRequestWrapper wrappedRequest = new HeaderMapRequestWrapper(request);
            if (sid != null) wrappedRequest.addHeader("X-User-Id", sid);
            if (restaurantId != null) wrappedRequest.addHeader("X-Restaurant-Id", String.valueOf(restaurantId));
            if (tableNumber != null) wrappedRequest.addHeader("X-Table-No", String.valueOf(tableNumber));
            if (deviceId != null) wrappedRequest.addHeader("X-Device-Id", deviceId);

            filterChain.doFilter(wrappedRequest, response);

        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    private boolean isPublicPath(String path) {
        return path.contains("/health") || 
               path.contains("/login") || 
               path.contains("/register") || 
               path.contains("/session/start");
    }

    /**
     * Helper class to allow adding headers to HttpServletRequest.
     */
    private static class HeaderMapRequestWrapper extends jakarta.servlet.http.HttpServletRequestWrapper {
        private final java.util.Map<String, String> headerMap = new java.util.HashMap<>();

        public HeaderMapRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        public void addHeader(String name, String value) {
            headerMap.put(name, value);
        }

        @Override
        public String getHeader(String name) {
            String headerValue = super.getHeader(name);
            if (headerValue == null) {
                headerValue = headerMap.get(name);
            }
            return headerValue;
        }

        @Override
        public java.util.Enumeration<String> getHeaderNames() {
            java.util.List<String> names = java.util.Collections.list(super.getHeaderNames());
            names.addAll(headerMap.keySet());
            return java.util.Collections.enumeration(names);
        }

        @Override
        public java.util.Enumeration<String> getHeaders(String name) {
            java.util.List<String> values = java.util.Collections.list(super.getHeaders(name));
            if (headerMap.containsKey(name)) {
                values.add(headerMap.get(name));
            }
            return java.util.Collections.enumeration(values);
        }
    }
}