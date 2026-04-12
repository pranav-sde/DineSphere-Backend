package com.festora.orderservice.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.festora.orderservice.dto.MenuItemRedis;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class MenuRedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String MENU_KEY_PREFIX = "menu:item:";

    public Optional<MenuItemRedis> getMenuItem(String menuItemId) {
        String key = MENU_KEY_PREFIX + menuItemId;
        try {
            Object raw = redisTemplate.opsForValue().get(key);
            if (raw == null) return Optional.empty();

            return Optional.ofNullable(objectMapper.convertValue(raw, MenuItemRedis.class));
        } catch (Exception e) {
            log.error("Failed to fetch menu item {} from Redis: {}", menuItemId, e.getMessage());
            return Optional.empty();
        }
    }
}
