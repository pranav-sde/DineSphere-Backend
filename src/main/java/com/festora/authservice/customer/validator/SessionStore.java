package com.festora.authservice.customer.validator;

import com.festora.authservice.customer.dto.SessionData;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SessionStore {

    private final StringRedisTemplate redis;

    public SessionData get(String sessionId) {
        String key = "session:" + sessionId;

        if (!redis.hasKey(key)) {
            return null;
        }

        Object restaurant = redis.opsForHash().get(key, "restaurantId");
        Object table = redis.opsForHash().get(key, "tableNumber");
        try {
            if (ObjectUtils.isNotEmpty(restaurant) && ObjectUtils.isNotEmpty(table)) {
                Long restaurantId = Long.parseLong(restaurant.toString());
                Integer tableNumber = Integer.parseInt(table.toString());
                return new SessionData(sessionId, restaurantId, tableNumber);
            }
        }
        catch (Exception ignore) {
            System.out.println("NumberFormat Exception : " + ignore.getMessage());
        }
        return null;
    }
}

