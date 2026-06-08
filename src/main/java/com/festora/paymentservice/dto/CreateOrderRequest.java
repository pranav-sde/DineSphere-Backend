package com.festora.paymentservice.dto;

import lombok.Data;

@Data
public class CreateOrderRequest {
    private String orderId;
    private double amount; // amount in rupees, will be converted to paise
    private String currency = "INR";
}
