package com.festora.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderResponse {
    private String razorpayOrderId;
    private int amount; // amount in paise
    private String currency;
    private String keyId; // public key for frontend checkout
}
