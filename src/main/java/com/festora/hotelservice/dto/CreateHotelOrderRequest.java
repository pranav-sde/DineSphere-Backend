package com.festora.hotelservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.festora.orderservice.model.OrderItem;
import lombok.Data;

import java.util.List;

/**
 * Request payload when a hotel guest confirms their identity and places an order.
 * hotelConfigId + mobileNumber + roomNumber is the guest's identity for this order.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateHotelOrderRequest {

    private String hotelConfigId;           // Which hotel
    private String mobileNumber;            // Guest self-declared mobile
    private String roomNumber;              // Guest self-declared room (free text or validated)
    private String paymentMode;             // ONLINE or CASH_ON_DELIVERY

    private List<OrderItem> items;
}