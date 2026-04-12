package com.festora.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SessionDto {
    private String sessionId;
    private String restaurantId;
    private Integer tableNumber;
    private String sessionToken;
    private long expiresAt;
}
