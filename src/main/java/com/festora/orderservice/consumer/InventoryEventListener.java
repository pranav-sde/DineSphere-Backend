package com.festora.orderservice.consumer;

import com.festora.orderservice.dto.InventoryReserveRequest;
import com.festora.orderservice.dto.event.InventoryConsumerEvent;
import com.festora.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventListener {
//    private final OrderService orderService;
//
//    @KafkaListener(topics = "inventory.reservation-events", groupId = "order-group")
//    public void onTempReserved(InventoryConsumerEvent event) {
//        if (ObjectUtils.isEmpty(event)) {
//            System.out.println("Inventory reservation request is empty");
//            return;
//        }
//        log.info("Inventory reserved for order {}", event.getOrderId());
//        orderService.markInventoryBasedOnStatus(event);
//    }
//
//    @KafkaListener(topics = "payment.events", groupId = "order-group")
//    public void paymentEvents(InventoryReserveRequest event) {
//        // TODO : Working on Payment service.
//    }
}
