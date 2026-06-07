package com.festora.orderservice.dto;

import com.festora.orderservice.enums.SeatingType;
import lombok.Data;

@Data
public class GenerateBillRequest {
    private Long restaurantId;
    private int tableNumber;
    private SeatingType seatingType;
    private String userId;
}
