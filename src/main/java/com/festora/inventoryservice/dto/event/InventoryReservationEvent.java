package com.festora.inventoryservice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class InventoryReservationEvent {
    private String orderId;
    private Long restaurantId;
    private String reservationId;
    private String status;
    private long expiresAt;
    private String reason;
}

