package com.festora.menuservice.service;

import com.festora.menuservice.entity.MenuItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class MenuRedisSyncService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String MENU_KEY_PREFIX = "menu:item:";

    public void syncMenuItem(MenuItem item) {
        if (item == null || item.getId() == null) return;

        String key = MENU_KEY_PREFIX + item.getId();
        try {
            // We store the entity as JSON. 
            // All services sharing the same Redis will be able to read this.
            redisTemplate.opsForValue().set(key, item, Duration.ofDays(2));
            log.info("Synced menu item {} to Redis", item.getId());
        } catch (Exception e) {
            log.error("Failed to sync menu item {} to Redis: {}", item.getId(), e.getMessage());
        }
    }

    public void removeMenuItem(String menuItemId) {
        String key = MENU_KEY_PREFIX + menuItemId;
        redisTemplate.delete(key);
        log.info("Removed menu item {} from Redis", menuItemId);
    }
}
