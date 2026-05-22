package com.festora.hotelservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Represents a hotel/palace/resort linked to a restaurant for room-service ordering.
 * Each hotel has a single QR code that all guests scan.
 */
@Document(collection = "hotel_configs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotelConfig {

    @Id
    private String id;

    @Indexed
    private Long restaurantId;

    // Hotel details
    private String hotelName;
    private String hotelType;           // HOTEL, PALACE, RESORT
    private String ownerName;
    private String mobile;
    private String email;
    private String address;
    private String city;
    private Integer totalRooms;

    // Room validation toggle
    @Builder.Default
    private boolean roomValidationEnabled = false;

    // QR — single unique QR per hotel
    @Indexed(unique = true)
    private String qrId;

    @Builder.Default
    private Boolean active = true;

    private long createdAt;
}
