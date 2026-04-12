package com.festora.orderservice.producer;

import com.festora.orderservice.dto.event.OrderCancelledProducerEvent;
import com.festora.orderservice.dto.event.OrderCreatedProduceEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEventProduce {
//    private final KafkaTemplate<String, Object> kafkaTemplate;
//
//    public void publishOrderCreatedEvent(OrderCreatedProduceEvent createdOrderEvent) {
//        kafkaTemplate.send("order.created",createdOrderEvent.getOrderId() , createdOrderEvent);
//    }
//
//    public void publishOrderCancelledEvent(OrderCancelledProducerEvent event) {
//        kafkaTemplate.send("order.cancelled", event.getOrderId(), event);
//    }
}