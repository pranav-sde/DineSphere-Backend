package com.festora.hotelservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * Stores room numbers for a specific floor of a hotel.
 * Used for optional room validation when roomValidationEnabled is true on HotelConfig.
 */
@Document(collection = "hotel_room_configs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotelRoomConfig {

    @Id
    private String id;

    @Indexed
    private String hotelConfigId;

    private String floorLabel;          // "Ground Floor", "Floor 1", "Suites"
    private int floorNumber;            // For ordering: 0, 1, 2...

    private String roomPrefix;
    private Integer startRoom;
    private Integer endRoom;

    private List<String> roomNumbers;   // ["101","102","103","104","105"]
}
