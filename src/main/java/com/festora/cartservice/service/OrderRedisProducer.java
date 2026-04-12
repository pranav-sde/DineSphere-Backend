package com.festora.cartservice.service;

import com.festora.cartservice.dto.client.OrderCreateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderRedisProducer {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String ORDER_QUEUE_KEY = "order:requests";

    public void submitOrder(OrderCreateRequest request) {
        try {
            // We use a Redis List as a simple reliable queue.
            // Right-push to the queue.
            redisTemplate.opsForList().rightPush(ORDER_QUEUE_KEY, request);
            
            // Set 24h expiry on the queue key itself 
            redisTemplate.expire(ORDER_QUEUE_KEY, Duration.ofHours(24));
            
            log.info("Order {} submitted to Redis queue", request.getOrderId());
        } catch (Exception e) {
            log.error("Failed to submit order {} to Redis: {}", request.getOrderId(), e.getMessage());
            throw new RuntimeException("Order submission failed", e);
        }
    }
}
