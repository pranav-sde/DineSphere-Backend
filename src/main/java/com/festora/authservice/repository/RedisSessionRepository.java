package com.festora.authservice.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
//import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Repository
public class RedisSessionRepository {

    private final RedisTemplate<String, String> redis;
    private final ObjectMapper mapper;
    private final String prefix = "session:";

    public RedisSessionRepository(RedisTemplate<String, String> redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
    }

    public void save(String sessionId, Map<String, Object> payload, long ttlSeconds) {
        try {
            String key = prefix + sessionId;
            String json = mapper.writeValueAsString(payload);
            redis.opsForValue().set(key, json, Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<Map<String, Object>> find(String sessionId) {
        try {
            String key = prefix + sessionId;
            String json = redis.opsForValue().get(key);
            if (json == null) return Optional.empty();
            var map = mapper.readValue(json, Map.class);
            return Optional.of(map);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(String sessionId) {
        redis.delete(prefix + sessionId);
    }
}