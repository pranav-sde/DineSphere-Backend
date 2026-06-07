package com.festora.paymentservice.controller;

import com.festora.paymentservice.dto.CreateOrderRequest;
import com.festora.paymentservice.dto.CreateOrderResponse;
import com.festora.paymentservice.dto.CreatePaymentRequest;
import com.festora.paymentservice.dto.VerifyPaymentRequest;
import com.festora.paymentservice.service.PaymentService;
import com.festora.paymentservice.service.RazorpayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import com.festora.paymentservice.dto.SubscriptionOrderRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.util.Map;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final RazorpayService razorpayService;


    @PostMapping
    public ResponseEntity<Map<String, String>> createPayment(
            @RequestBody CreatePaymentRequest req
    ) {

        String paymentId = paymentService.createPayment(
                req.getOrderId(),
                req.getAmount(),
                "INR",
                req.getPaymentMode(),
                req.getPaymentMethod()
        );

        return ResponseEntity.ok(
                Map.of(
                        "paymentId", paymentId,
                        "status", "SUCCESS"
                )
        );
    }

    @PostMapping("/subscription/create-order")
    public ResponseEntity<?> createSubscriptionOrder(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestBody SubscriptionOrderRequest request
    ) {
        try {
            if (userId == null || userId.isBlank()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
            }
            if (request.getPlanId() == null || request.getPlanId().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "planId is required"));
            }
            CreateOrderResponse response = razorpayService.createSubscriptionOrder(userId, request.getPlanId());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid subscription create-order request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Error creating Razorpay subscription order", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create subscription order"));
        }
    }

    @PostMapping("/subscription/verify-payment")
    public ResponseEntity<?> verifySubscriptionPayment(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestBody VerifyPaymentRequest request
    ) {
        try {
            if (userId == null || userId.isBlank()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
            }
            Map<String, String> result = razorpayService.verifySubscriptionPayment(request);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid subscription verify-payment request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            log.warn("Payment verification failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Error verifying payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Payment verification failed"));
        }
    }
}
