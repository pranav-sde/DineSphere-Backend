package com.festora.orderservice.dto;

import lombok.Data;

@Data
public class GenerateHotelBillRequest {
    private Long restaurantId;
    private String hotelConfigId;
    private String roomNumber;
    private String userId;
}
