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

    // Seating & source
    private String seatingType;       // TABLE, ROOM, HOTEL_ROOM
    private String orderSource;       // DINE_IN, HOTEL_ROOM_SERVICE

    // Hotel-specific
    private String hotelConfigId;
    private String mobileNumber;
    private String roomNumber;
    private String paymentMode;       // ONLINE, CASH_ON_DELIVERY

    private List<OrderItem> items;
    private double subtotal;
}

