package com.festora.hotelservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateHotelRequest {
    private String hotelName;
    private String hotelType;       // HOTEL, PALACE, RESORT
    private String ownerName;
    private String mobile;
    private String email;
    private String address;
    private String city;
    private Integer totalRooms;
    private boolean roomValidationEnabled;
}