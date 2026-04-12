package com.festora.authservice.dto;

import lombok.Builder;

@Builder
public record SessionResult(String sessionId, String restaurantId, Integer tableNumber, String sessionToken, long expiresAt) { }
