package com.festora.cartservice.dto.client;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OrderCreateRequest {

    private String orderId;
    private Long restaurantId;
    private String userId;
    private String deviceId;
    private int tableNumber;
    private List<OrderItem> items;
    private double subtotal;
}
