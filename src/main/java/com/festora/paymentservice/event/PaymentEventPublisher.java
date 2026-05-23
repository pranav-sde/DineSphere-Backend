package com.festora.paymentservice.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PaymentEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public PaymentEventPublisher(@Autowired(required = false) KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String topic, String key, String payload) {
        if (kafkaTemplate != null) {
            kafkaTemplate.send(topic, key, payload);
        } else {
            log.info("Kafka is disabled. Payment event not published to Kafka: topic={}, key={}, payload={}", topic, key, payload);
        }
    }
}

