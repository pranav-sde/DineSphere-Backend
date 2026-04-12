package com.festora.cartservice.repository;

import com.festora.cartservice.model.Cart;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class CartRedisRepository {

    private final RedisTemplate<String, Cart> redisTemplate;
    private static final long TTL_SECONDS = 2700;

    public Cart get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void save(String key, Cart cart) {
        redisTemplate.opsForValue()
                .set(key, cart, TTL_SECONDS, TimeUnit.SECONDS);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }
}