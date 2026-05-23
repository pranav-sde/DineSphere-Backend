package com.festora.paymentservice.model;

import com.festora.paymentservice.enums.PaymentMethod;
import com.festora.paymentservice.enums.PaymentMode;
import com.festora.paymentservice.enums.PaymentStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "payment_ledger")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentLedger {

    @Id
    private String paymentId;

    @Indexed
    private String orderId;

    private double amount;

    private String currency;

    private PaymentMode paymentMode;

    private PaymentMethod paymentMethod;

    private PaymentStatus status;

    private long createdAt;

    @Indexed
    private String razorpayOrderId;

    private String razorpayPaymentId;

    private String razorpaySignature;
}
