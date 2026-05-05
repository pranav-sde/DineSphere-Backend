package com.festora.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SessionStartResponse {
    private Integer tableNumber;
    private String sessionToken;
    private String refreshToken;
    private long expiresIn;
}
