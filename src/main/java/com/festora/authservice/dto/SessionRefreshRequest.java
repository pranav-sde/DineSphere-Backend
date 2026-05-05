package com.festora.authservice.dto;

import lombok.Data;

@Data
public class SessionRefreshRequest {
    private String refreshToken;
}
