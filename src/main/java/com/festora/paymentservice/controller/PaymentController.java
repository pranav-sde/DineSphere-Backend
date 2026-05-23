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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.festora.orderservice.service.OrderService;

import java.util.Map;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final RazorpayService razorpayService;
    private final OrderService orderService;

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

    /**
     * Creates a Razorpay order for the given amount and orderId.
     * Returns the razorpayOrderId, amount (paise), currency, and public keyId.
     */
    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody CreateOrderRequest request) {
        try {
            CreateOrderResponse response = razorpayService.createOrder(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid create-order request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Error creating Razorpay order", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create order"));
        }
    }

    /**
     * Verifies the Razorpay payment signature and updates the ledger.
     * Returns success only if the HMAC-SHA256 signature matches.
     */
    @PostMapping("/verify-payment")
    public ResponseEntity<?> verifyPayment(@RequestBody VerifyPaymentRequest request) {
        try {
            Map<String, String> result = razorpayService.verifyPayment(request);
            
            // On successful verification, immediately update the order status
            String orderId = result.get("orderId");
            if (orderId != null) {
                orderService.onPaymentSuccess(orderId, "ONLINE", request.getRazorpayPaymentId());
            }
            
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid verify-payment request: {}", e.getMessage());
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

    /**
     * Cancels a Razorpay payment ledger entry.
     */
    @PostMapping("/cancel")
    public ResponseEntity<?> cancelPayment(@RequestBody Map<String, String> request) {
        String razorpayOrderId = request.get("razorpayOrderId");
        if (razorpayOrderId != null) {
            razorpayService.cancelPayment(razorpayOrderId);
        }
        return ResponseEntity.ok(Map.of("status", "CANCELLED"));
    }
}
