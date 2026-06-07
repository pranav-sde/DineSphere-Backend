package com.festora.authservice.service;

import com.festora.paymentservice.event.SubscriptionPaymentSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionPaymentListener {

    private final AdminUserService adminUserService;

    @EventListener
    public void onSubscriptionPaymentSuccess(SubscriptionPaymentSuccessEvent event) {
        log.info("Received SubscriptionPaymentSuccessEvent: userId={}, months={}, paymentId={}",
                event.getUserId(), event.getMonths(), event.getPaymentId());
        
        try {
            adminUserService.renewSubscription(event.getUserId(), event.getMonths(), event.getPlanId());
            log.info("Subscription successfully renewed for userId={} with plan={}", event.getUserId(), event.getPlanId());
        } catch (Exception e) {
            log.error("Failed to renew subscription for userId={} after successful payment", event.getUserId(), e);
        }
    }
}
