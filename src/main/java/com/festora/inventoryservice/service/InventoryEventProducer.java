package com.festora.inventoryservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryEventProducer {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String ORDER_EVENT_QUEUE = "order:events";

    public void notifyStatus(String orderId, String status) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("orderId", orderId);
            event.put("status", status);
            event.put("timestamp", System.currentTimeMillis());

            redisTemplate.opsForList().rightPush(ORDER_EVENT_QUEUE, event);
            log.info("Inventory status {} notified for order {} to Redis", status, orderId);
        } catch (Exception e) {
            log.error("Failed to notify inventory status for order {}: {}", orderId, e.getMessage());
        }
    }
}
