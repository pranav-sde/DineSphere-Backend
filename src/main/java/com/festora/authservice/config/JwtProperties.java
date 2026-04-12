package com.festora.authservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String issuer;
    private String audience;

    private String publicKey;
    private String privateKey;

    private long accessTokenTtlMinutes;
}