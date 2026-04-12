package com.festora.orderservice.dto;

import com.festora.orderservice.enums.OrderStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderCreateResponse {

    private String orderId;
    private OrderStatus status;
    private double totalAmount;
}

