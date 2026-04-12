package com.festora.orderservice.enums;

public enum OrderStatus {
    CREATED,
    REJECTED,
    PENDING,
    PAID,
    PREPARING,
    CLOSED,
    PAYMENT_PENDING,
    PAYMENT_REQUESTED,  // Bill frozen, waiting for payment
    CANCELLED
}

