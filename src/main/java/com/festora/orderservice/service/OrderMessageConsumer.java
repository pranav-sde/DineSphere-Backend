package com.festora.orderservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.festora.orderservice.dto.CreateOrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderMessageConsumer {

    private final RedisTemplate<String, Object> redisTemplate;
    private final OrderService orderService;
    private final ObjectMapper objectMapper;
    private static final String ORDER_QUEUE_KEY = "order:requests";

    /**
     * Poll Redis for new order requests every second.
     * In a production environment, you might use Redis Streams for better reliability.
     */
    @Scheduled(fixedDelay = 1000)
    public void consumeOrders() {
        try {
            // Left-pop from the queue using non-blocking LPOP.
            // Upstash and some proxy-based Redis providers don't natively support blocking commands like BLPOP.
            Object raw = redisTemplate.opsForList().leftPop(ORDER_QUEUE_KEY);
            
            if (raw != null) {
                log.info("Received order request from Redis");
                CreateOrderRequest request = objectMapper.convertValue(raw, CreateOrderRequest.class);
                orderService.createOrder(request);
                log.info("Successfully processed order {}", request.getOrderId());
            }
        } catch (Exception e) {
            log.error("Error processing order from Redis: {}", e.getMessage(), e);
        }
    }
}
