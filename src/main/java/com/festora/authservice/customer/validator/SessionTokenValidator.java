package com.festora.authservice.customer.validator;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;

@Component
public class SessionTokenValidator {

    @Value("${app.session.secret}")
    private String secret;

    private Key key;

    @PostConstruct
    void init() {
        key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public Claims validate(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}

