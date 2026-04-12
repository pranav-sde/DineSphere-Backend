package com.festora.orderservice.service;

import com.festora.orderservice.enums.OrderStatus;
import com.festora.orderservice.model.Order;
import com.festora.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryEventConsumer {

    private final RedisTemplate<String, Object> redisTemplate;
    private final OrderRepository orderRepository;
    private static final String ORDER_EVENT_QUEUE = "order:events";

    @Scheduled(fixedDelay = 1000)
    public void consumeInventoryEvents() {
        try {
            Object raw = redisTemplate.opsForList().leftPop(ORDER_EVENT_QUEUE, 1, TimeUnit.SECONDS);
            
            if (raw != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> event = (Map<String, Object>) raw;
                String orderId = (String) event.get("orderId");
                String status = (String) event.get("status");

                log.info("Received inventory event for order {}: {}", orderId, status);

                Order order = orderRepository.findByOrderId(orderId);
                if (order == null) {
                    log.warn("Order {} not found for inventory event", orderId);
                    return;
                }

                if ("TEMP_RESERVED".equals(status)) {
                    order.setStatus(OrderStatus.PENDING);
                } else if ("OUT_OF_STOCK".equals(status)) {
                    order.setStatus(OrderStatus.REJECTED);
                    order.setReason("OUT_OF_STOCK");
                }

                order.setUpdatedAt(System.currentTimeMillis());
                orderRepository.save(order);
                log.info("Order {} status updated to {} based on inventory event", orderId, order.getStatus());
            }
        } catch (Exception e) {
            log.error("Error processing inventory event from Redis: {}", e.getMessage(), e);
        }
    }
}
