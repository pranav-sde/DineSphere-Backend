package com.festora.orderservice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryConsumerEvent {
    private String orderId;
    private Long restaurantId;
    private String reservationId;
    private String status;
    private String expiresAt;
    private String reason;
}