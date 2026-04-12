package com.festora.authservice.security;

import com.festora.authservice.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import java.security.PublicKey;

@Component
public class JwtValidator {

    private final PublicKey publicKey;
    private final JwtProperties props;

    public JwtValidator(PublicKey publicKey, JwtProperties props) {
        this.publicKey = publicKey;
        this.props = props;
    }

    public Jws<Claims> validate(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .requireIssuer(props.getIssuer())
                .requireAudience(props.getAudience())
                .build()
                .parseClaimsJws(token);
    }
}