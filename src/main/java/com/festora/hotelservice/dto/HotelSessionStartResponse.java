package com.festora.hotelservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response when a hotel guest scans the hotel QR code.
 * Contains hotel info + lightweight session tokens for menu browsing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotelSessionStartResponse {
    private Long restaurantId;
    private String hotelConfigId;
    private String hotelName;
    private String hotelType;
    private String seatingType;             // Always "HOTEL_ROOM"
    private boolean roomValidationEnabled;
    private String sessionToken;
    private String refreshToken;
    private long expiresIn;
}