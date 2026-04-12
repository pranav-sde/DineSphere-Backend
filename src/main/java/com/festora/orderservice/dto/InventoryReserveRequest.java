package com.festora.orderservice.dto;

import com.festora.orderservice.model.Order;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class InventoryReserveRequest {

    private String orderId;
    private Long restaurantId;
    private int ttlSeconds;
    private List<InventoryItem> items;

    public static InventoryReserveRequest from(Order order, int ttlSeconds) {
        return InventoryReserveRequest.builder()
                .orderId(order.getOrderId())
                .restaurantId(order.getRestaurantId())
                .ttlSeconds(ttlSeconds)
                .items(
                        order.getItems().stream()
                                .map(i -> new InventoryItem(
                                        i.getMenuItemId(),
                                        i.getVariantId(),
                                        i.getQuantity()
                                ))
                                .toList()
                )
                .build();
    }
}
