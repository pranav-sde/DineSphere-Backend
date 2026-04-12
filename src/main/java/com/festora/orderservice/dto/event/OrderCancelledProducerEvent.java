package com.festora.orderservice.dto.event;

import com.festora.orderservice.model.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelledProducerEvent {
    private String orderId;
    private Long restaurantId;
    private List<OrderItem> items;
}