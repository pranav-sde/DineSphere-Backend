package com.festora.inventoryservice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedConsumerEvent {
    private String orderId;
    private Long restaurantId;
}