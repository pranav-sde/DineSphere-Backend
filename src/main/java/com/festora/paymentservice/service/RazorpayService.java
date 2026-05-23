package com.festora.paymentservice.service;

import com.festora.paymentservice.config.RazorpayConfig;
import com.festora.paymentservice.dto.CreateOrderRequest;
import com.festora.paymentservice.dto.CreateOrderResponse;
import com.festora.paymentservice.dto.VerifyPaymentRequest;
import com.festora.paymentservice.enums.PaymentMethod;
import com.festora.paymentservice.enums.PaymentMode;
import com.festora.paymentservice.enums.PaymentStatus;
import com.festora.paymentservice.model.PaymentLedger;
import com.festora.paymentservice.model.PaymentOutbox;
import com.festora.paymentservice.repository.PaymentLedgerRepository;
import com.festora.paymentservice.repository.PaymentOutboxRepository;

import com.festora.orderservice.service.OrderService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@Slf4j
public class RazorpayService {

    private final RazorpayClient razorpayClient;
    private final RazorpayConfig razorpayConfig;
    private final PaymentLedgerRepository ledgerRepo;
    private final PaymentOutboxRepository outboxRepo;
    private final OrderService orderService;

    public RazorpayService(
            @Autowired(required = false) RazorpayClient razorpayClient,
            RazorpayConfig razorpayConfig,
            PaymentLedgerRepository ledgerRepo,
            PaymentOutboxRepository outboxRepo,
            OrderService orderService
    ) {
        this.razorpayClient = razorpayClient;
        this.razorpayConfig = razorpayConfig;
        this.ledgerRepo = ledgerRepo;
        this.outboxRepo = outboxRepo;
        this.orderService = orderService;
    }

    /**
     * Creates a Razorpay order and persists a PENDING ledger entry.
     */
    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        if (razorpayClient == null) {
            throw new IllegalStateException("Razorpay is not configured. Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET environment variables.");
        }
        
        // Securely fetch order from DB to prevent tampering
        com.festora.orderservice.model.Order dbOrder = orderService.getOrder(request.getOrderId());
        if (dbOrder == null) {
            throw new IllegalArgumentException("Order not found: " + request.getOrderId());
        }

        // Convert amount from rupees to paise
        double amountInRupees = dbOrder.getTotalAmount();
        int amountInPaise = (int) Math.round(amountInRupees * 100);

        if (amountInPaise < 100) {
            throw new IllegalArgumentException("Minimum amount is ₹1 (100 paise). Received: " + amountInPaise + " paise");
        }

        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", request.getCurrency() != null ? request.getCurrency() : "INR");
            orderRequest.put("receipt", "receipt_" + request.getOrderId());

            Order razorpayOrder = razorpayClient.orders.create(orderRequest);

            String razorpayOrderId = razorpayOrder.get("id");
            int amount = razorpayOrder.get("amount");
            String currency = razorpayOrder.get("currency");

            // Persist ledger entry in ATTEMPTED state
            PaymentLedger ledger = PaymentLedger.builder()
                    .orderId(request.getOrderId())
                    .amount(amountInRupees)
                    .currency(currency)
                    .paymentMode(PaymentMode.PREPAID)
                    .paymentMethod(PaymentMethod.RAZORPAY)
                    .status(PaymentStatus.ATTEMPTED)
                    .razorpayOrderId(razorpayOrderId)
                    .createdAt(System.currentTimeMillis())
                    .build();

            ledgerRepo.save(ledger);

            log.info("Razorpay order created: {} for orderId: {}", razorpayOrderId, request.getOrderId());

            return CreateOrderResponse.builder()
                    .razorpayOrderId(razorpayOrderId)
                    .amount(amount)
                    .currency(currency)
                    .keyId(razorpayConfig.getKeyId())
                    .build();

        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order for orderId: {}", request.getOrderId(), e);
            throw new RuntimeException("Failed to create Razorpay order: " + e.getMessage(), e);
        }
    }

    /**
     * Verifies the Razorpay payment signature using HMAC-SHA256 and updates the ledger.
     */
    @Transactional
    public Map<String, String> verifyPayment(VerifyPaymentRequest request) {
        if (request.getRazorpayOrderId() == null || request.getRazorpayPaymentId() == null
                || request.getRazorpaySignature() == null) {
            throw new IllegalArgumentException("Missing required fields: razorpayOrderId, razorpayPaymentId, razorpaySignature");
        }

        // Find the pending ledger entry
        PaymentLedger ledger = ledgerRepo.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No payment found for Razorpay order: " + request.getRazorpayOrderId()));

        // Compute HMAC-SHA256 signature
        String payload = request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();
        String generatedSignature = computeHmacSha256(payload, razorpayConfig.getKeySecret());

        if (generatedSignature != null && generatedSignature.equals(request.getRazorpaySignature())) {
            // Signature matches — payment is authentic
            ledger.setStatus(PaymentStatus.SUCCESS);
            ledger.setRazorpayPaymentId(request.getRazorpayPaymentId());
            ledger.setRazorpaySignature(request.getRazorpaySignature());
            ledgerRepo.save(ledger);

            // Create outbox event for downstream consumers
            PaymentOutbox outbox = PaymentOutbox.builder()
                    .eventType("payment.success")
                    .aggregateId(ledger.getOrderId())
                    .payload(buildPayload(ledger))
                    .published(false)
                    .createdAt(System.currentTimeMillis())
                    .build();
            outboxRepo.save(outbox);

            log.info("Payment verified successfully for orderId: {}, razorpayPaymentId: {}",
                    ledger.getOrderId(), request.getRazorpayPaymentId());

            return Map.of(
                    "status", "SUCCESS",
                    "paymentId", ledger.getPaymentId(),
                    "orderId", ledger.getOrderId()
            );
        } else {
            // Signature mismatch — do NOT mark as paid
            ledger.setStatus(PaymentStatus.FAILED);
            ledgerRepo.save(ledger);

            // Create failed outbox event
            PaymentOutbox outbox = PaymentOutbox.builder()
                    .eventType("payment.failed")
                    .aggregateId(ledger.getOrderId())
                    .payload(buildPayload(ledger))
                    .published(false)
                    .createdAt(System.currentTimeMillis())
                    .build();
            outboxRepo.save(outbox);

            log.warn("Payment signature mismatch for razorpayOrderId: {}", request.getRazorpayOrderId());

            throw new SecurityException("Payment signature verification failed");
        }
    }

    /**
     * Marks a Razorpay payment ledger entry as CANCELLED.
     */
    @Transactional
    public void cancelPayment(String razorpayOrderId) {
        if (razorpayOrderId == null) return;
        PaymentLedger ledger = ledgerRepo.findByRazorpayOrderId(razorpayOrderId).orElse(null);
        if (ledger != null && ledger.getStatus() == PaymentStatus.ATTEMPTED) {
            ledger.setStatus(PaymentStatus.CANCELLED);
            ledgerRepo.save(ledger);
            log.info("Payment cancelled for razorpayOrderId: {}", razorpayOrderId);
        }
    }

    private String computeHmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Error computing HMAC-SHA256", e);
            return null;
        }
    }

    private String buildPayload(PaymentLedger ledger) {
        return String.format("""
            {
              "orderId": "%s",
              "paymentId": "%s",
              "amount": %.2f,
              "currency": "%s",
              "paymentMode": "%s",
              "method": "%s",
              "razorpayOrderId": "%s",
              "razorpayPaymentId": "%s",
              "timestamp": %d
            }
            """,
                ledger.getOrderId(),
                ledger.getPaymentId(),
                ledger.getAmount(),
                ledger.getCurrency(),
                ledger.getPaymentMode(),
                ledger.getPaymentMethod(),
                ledger.getRazorpayOrderId() != null ? ledger.getRazorpayOrderId() : "",
                ledger.getRazorpayPaymentId() != null ? ledger.getRazorpayPaymentId() : "",
                ledger.getCreatedAt()
        );
    }
}
