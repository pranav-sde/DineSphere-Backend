package com.festora.authservice.customer.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.festora.authservice.customer.dto.SessionData;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SessionStore {

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public SessionData get(String sessionId) {
        String key = "session:" + sessionId;

        String json = redis.opsForValue().get(key);
        if (json == null || json.isBlank()) {
            return null;
        }

        try {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> data = mapper.readValue(json, java.util.Map.class);

            Object restaurantRaw = data.get("restaurantId");
            Object tableRaw     = data.get("tableNumber");

            if (restaurantRaw == null || tableRaw == null) return null;

            Long    restaurantId = Long.parseLong(restaurantRaw.toString());
            Integer tableNumber  = Integer.parseInt(tableRaw.toString());

            return new SessionData(sessionId, restaurantId, tableNumber);

        } catch (Exception e) {
            System.out.println("SessionStore parse error: " + e.getMessage());
            return null;
        }
    }
}
