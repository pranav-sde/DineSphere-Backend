package com.festora.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SessionStartResponse {
    private Integer tableNumber;
    private String seatingType;
    private String sessionToken;
    private String refreshToken;
    private long expiresIn;
    private String hotelConfigId;
}
