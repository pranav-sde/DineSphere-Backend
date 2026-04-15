package com.festora.authservice.security;

import com.festora.authservice.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.PublicKey;
import java.util.Base64;

@Component
public class JwtValidator {

    private final PublicKey publicKey;
    private final JwtProperties props;
    private final Key qrHmacKey;
    private final Key sessionHmacKey;

    public JwtValidator(PublicKey publicKey,
                        JwtProperties props,
                        @Value("${app.qr.secret}") String qrSecret,
                        @Value("${app.session.secret}") String sessionSecret) {
        this.publicKey = publicKey;
        this.props = props;
        this.qrHmacKey = Keys.hmacShaKeyFor(qrSecret.getBytes(StandardCharsets.UTF_8));
        this.sessionHmacKey = Keys.hmacShaKeyFor(sessionSecret.getBytes(StandardCharsets.UTF_8));
    }

    public Jws<Claims> validate(String token) {
        String alg = peekAlgorithm(token);

        if (alg != null && alg.startsWith("RS")) {
            // Admin / Owner — RSA signed token
            return Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .requireIssuer(props.getIssuer())
                    .requireAudience(props.getAudience())
                    .build()
                    .parseClaimsJws(token);
        }

        // Customer token — HMAC signed.
        // Try the QR secret first (SessionJwtUtil uses app.qr.secret).
        // Fall back to the session secret (SessionTokenValidator uses app.session.secret).
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(qrHmacKey)
                    .build()
                    .parseClaimsJws(token);
        } catch (Exception ignored) {}

        return Jwts.parserBuilder()
                .setSigningKey(sessionHmacKey)
                .build()
                .parseClaimsJws(token);
    }

    /**
     * Decode the JWT header (first segment) and read the "alg" field
     * without fully parsing the token. Returns null if the header cannot be read.
     */
    private String peekAlgorithm(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            // Simple extraction — avoids an extra JSON dependency
            int algIdx = headerJson.indexOf("\"alg\"");
            if (algIdx < 0) return null;
            int colon = headerJson.indexOf(':', algIdx);
            int start = headerJson.indexOf('"', colon) + 1;
            int end = headerJson.indexOf('"', start);
            return headerJson.substring(start, end);
        } catch (Exception e) {
            return null;
        }
    }
}