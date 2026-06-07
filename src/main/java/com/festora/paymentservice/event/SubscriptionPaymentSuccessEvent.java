package com.festora.paymentservice.event;

import lombok.Getter;

@Getter
public class SubscriptionPaymentSuccessEvent {
    private final String userId;
    private final int months;
    private final String paymentId;
    private final String planId;

    public SubscriptionPaymentSuccessEvent(String userId, int months, String paymentId, String planId) {
        this.userId = userId;
        this.months = months;
        this.paymentId = paymentId;
        this.planId = planId;
    }
}
