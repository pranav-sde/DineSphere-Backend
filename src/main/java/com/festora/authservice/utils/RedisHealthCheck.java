package com.festora.authservice.utils;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class RedisHealthCheck {
    private final RedisConnectionFactory factory;
    private final RedisTemplate<String, Object> redisTemplate;
    @Value("${spring.data.redis.url}")
    private String redisUrl;

    @PostConstruct
    public void checkRedis() {
        try {
            String pong = redisTemplate.execute((RedisCallback<String>) connection -> connection.ping());
            log.info("Redis PING response: {}", pong);
        } catch (Exception e) {
            log.error("Redis connection failed", e);
        }
    }
}

