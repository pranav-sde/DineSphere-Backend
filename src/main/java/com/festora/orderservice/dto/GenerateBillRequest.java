package com.festora.orderservice.dto;

import lombok.Data;

@Data
public class GenerateBillRequest {
    private Long restaurantId;
    private int tableNumber;
    private String userId;
}
