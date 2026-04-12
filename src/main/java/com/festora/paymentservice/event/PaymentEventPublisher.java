package com.festora.paymentservice.event;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void publish(String topic, String key, String payload) {
        kafkaTemplate.send(topic, key, payload);
    }
}

