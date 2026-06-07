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
import com.festora.paymentservice.config.SubscriptionPlanConfig;
import com.festora.paymentservice.event.SubscriptionPaymentSuccessEvent;
import org.springframework.context.ApplicationEventPublisher;

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
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class RazorpayService {

    private final RazorpayClient razorpayClient;
    private final RazorpayConfig razorpayConfig;
    private final PaymentLedgerRepository ledgerRepo;
    private final PaymentOutboxRepository outboxRepo;
    private final SubscriptionPlanConfig planConfig;
    private final ApplicationEventPublisher eventPublisher;


    public RazorpayService(
            @Autowired(required = false) RazorpayClient razorpayClient,
            RazorpayConfig razorpayConfig,
            PaymentLedgerRepository ledgerRepo,
            PaymentOutboxRepository outboxRepo,
            SubscriptionPlanConfig planConfig,
            ApplicationEventPublisher eventPublisher
    ) {
        this.razorpayClient = razorpayClient;
        this.razorpayConfig = razorpayConfig;
        this.ledgerRepo = ledgerRepo;
        this.outboxRepo = outboxRepo;
        this.planConfig = planConfig;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CreateOrderResponse createSubscriptionOrder(String userId, String planId) {
        if (razorpayClient == null) {
            throw new IllegalStateException("Razorpay is not configured. Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET environment variables.");
        }

        SubscriptionPlanConfig.PlanDetails plan = planConfig.getPlans().get(planId.toLowerCase());
        if (plan == null) {
            throw new IllegalArgumentException("Invalid subscription plan: " + planId);
        }

        double amountInRupees = plan.getPrice();
        int amountInPaise = (int) Math.round(amountInRupees * 100);

        try {
            String receipt = "rcpt_sub_" + UUID.randomUUID().toString().replace("-", "").substring(0, 15);
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", receipt);

            Order razorpayOrder = razorpayClient.orders.create(orderRequest);

            String razorpayOrderId = razorpayOrder.get("id");
            int amount = razorpayOrder.get("amount");
            String currency = razorpayOrder.get("currency");

            // Persist ledger entry in ATTEMPTED state
            PaymentLedger ledger = PaymentLedger.builder()
                    .amount(amountInRupees)
                    .currency(currency)
                    .paymentMode(PaymentMode.PREPAID)
                    .paymentMethod(PaymentMethod.RAZORPAY)
                    .status(PaymentStatus.ATTEMPTED)
                    .razorpayOrderId(razorpayOrderId)
                    .createdAt(System.currentTimeMillis())
                    .paymentType("SUBSCRIPTION")
                    .userId(userId)
                    .subscriptionPlanId(planId)
                    .subscriptionMonths(plan.getMonths())
                    .build();

            ledgerRepo.save(ledger);

            log.info("Razorpay subscription order created: {} for userId: {} (Plan: {})", razorpayOrderId, userId, planId);

            return CreateOrderResponse.builder()
                    .razorpayOrderId(razorpayOrderId)
                    .amount(amount)
                    .currency(currency)
                    .keyId(razorpayConfig.getKeyId())
                    .build();

        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay subscription order for userId: {}", userId, e);
            throw new RuntimeException("Failed to create Razorpay subscription order: " + e.getMessage(), e);
        }
    }

    @Transactional
    public Map<String, String> verifySubscriptionPayment(VerifyPaymentRequest request) {
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

        if (generatedSignature != null && MessageDigest.isEqual(
                generatedSignature.getBytes(StandardCharsets.UTF_8),
                request.getRazorpaySignature().getBytes(StandardCharsets.UTF_8))) {
            // Signature matches — payment is authentic
            ledger.setStatus(PaymentStatus.SUCCESS);
            ledger.setRazorpayPaymentId(request.getRazorpayPaymentId());
            ledger.setRazorpaySignature(request.getRazorpaySignature());
            ledgerRepo.save(ledger);

            // Create outbox event for downstream consumers
            PaymentOutbox outbox = PaymentOutbox.builder()
                    .eventType("subscription.payment.success")
                    .aggregateId(ledger.getUserId())
                    .payload(buildPayload(ledger))
                    .published(false)
                    .createdAt(System.currentTimeMillis())
                    .build();
            outboxRepo.save(outbox);

            log.info("Subscription payment verified successfully for userId: {}, plan: {}, paymentId: {}",
                    ledger.getUserId(), ledger.getSubscriptionPlanId(), request.getRazorpayPaymentId());

            // Publish application event for decoupled subscription renewal
            eventPublisher.publishEvent(new SubscriptionPaymentSuccessEvent(
                    ledger.getUserId(),
                    ledger.getSubscriptionMonths(),
                    ledger.getPaymentId(),
                    ledger.getSubscriptionPlanId()
            ));

            return Map.of(
                    "status", "SUCCESS",
                    "paymentId", ledger.getPaymentId(),
                    "userId", ledger.getUserId()
            );
        } else {
            // Signature mismatch — do NOT mark as paid
            ledger.setStatus(PaymentStatus.FAILED);
            ledgerRepo.save(ledger);

            log.warn("Subscription payment signature mismatch for razorpayOrderId: {}", request.getRazorpayOrderId());

            throw new SecurityException("Payment signature verification failed");
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
        JSONObject payload = new JSONObject();
        payload.put("orderId", ledger.getOrderId() != null ? ledger.getOrderId() : "");
        payload.put("paymentId", ledger.getPaymentId() != null ? ledger.getPaymentId() : "");
        payload.put("amount", ledger.getAmount());
        payload.put("currency", ledger.getCurrency() != null ? ledger.getCurrency() : "");
        payload.put("paymentMode", ledger.getPaymentMode() != null ? ledger.getPaymentMode().toString() : "");
        payload.put("method", ledger.getPaymentMethod() != null ? ledger.getPaymentMethod().toString() : "");
        payload.put("razorpayOrderId", ledger.getRazorpayOrderId() != null ? ledger.getRazorpayOrderId() : "");
        payload.put("razorpayPaymentId", ledger.getRazorpayPaymentId() != null ? ledger.getRazorpayPaymentId() : "");
        payload.put("timestamp", ledger.getCreatedAt());
        return payload.toString();
    }
}
