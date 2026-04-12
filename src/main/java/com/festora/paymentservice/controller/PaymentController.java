package com.festora.paymentservice.controller;

import com.festora.paymentservice.dto.CreatePaymentRequest;
import com.festora.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

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
}

