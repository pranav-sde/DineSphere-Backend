package com.festora.orderservice.dto;

import com.festora.orderservice.model.OrderItem;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderRequest {

    private String orderId;
    private Long restaurantId;
    private String userId;
    private String userName;
    private String deviceId;
    private int tableNumber;
    private List<OrderItem> items;
    private double subtotal;
}

