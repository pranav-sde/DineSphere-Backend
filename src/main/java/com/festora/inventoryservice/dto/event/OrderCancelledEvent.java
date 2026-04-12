package com.festora.inventoryservice.dto.event;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderCancelledEvent {
    private String orderId;
    private Long restaurantId;
}
