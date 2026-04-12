package com.festora.paymentservice.dto;

import com.festora.paymentservice.enums.PaymentMethod;
import com.festora.paymentservice.enums.PaymentMode;
import lombok.Data;

@Data
public class CreatePaymentRequest {
    private String orderId;
    private double amount;
    private PaymentMode paymentMode;
    private PaymentMethod paymentMethod;
}

