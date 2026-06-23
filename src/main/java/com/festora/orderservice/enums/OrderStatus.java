package com.festora.orderservice.enums;

public enum OrderStatus {
    CREATED,
    REJECTED,
    PENDING,
    PAID,
    PREPARING,
    IN_KITCHEN,
    READY_TO_SERVE,
    SERVED,
    CLOSED,
    PAYMENT_PENDING,
    PAYMENT_REQUESTED,  // Bill frozen, waiting for payment
    CANCELLED
}

