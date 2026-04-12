package com.festora.cartservice.controller;

import com.festora.cartservice.model.Cart;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedisController {

    private final RedisTemplate<String, Cart> redisTemplate;

    public RedisController(RedisTemplate<String, Cart> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/test/redis")
    public String test() {
        Cart cart = Cart.builder()
                .cartId("TEST_CART")
                .restaurantId(101L)
                .userId("TEST_SESSION")
                .subtotal(0)
                .build();

        redisTemplate.opsForValue().set("test:key", cart);
        return "OK";
    }
}