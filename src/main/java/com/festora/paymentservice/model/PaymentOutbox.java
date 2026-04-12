package com.festora.paymentservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "payment_outbox")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentOutbox {

    @Id
    private String eventId;

    private String eventType; // payment.success | payment.failed
    private String aggregateId; // orderId

    private String payload;

    private boolean published;
    private long createdAt;
}
