package com.festora.monolith.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Optimized Redis Utilities for Upstash/Valkey Performance.
 * 
 * H-methods (hput/hget): Uses Redis Hashes for efficient field-level access.
 * Z-methods (zput/zget): Uses GZIP compression to save bandwidth and requests.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisUtils {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Hash Put: Stores an object in a Redis Hash.
     */
    public void hput(String key, String hashKey, Object value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    /**
     * Hash Get: Retrieves an object from a Redis Hash.
     */
    public <T> T hget(String key, String hashKey, Class<T> type) {
        Object val = redisTemplate.opsForHash().get(key, hashKey);
        if (val == null) return null;
        return objectMapper.convertValue(val, type);
    }

    /**
     * Zipped Put: Compresses the object using GZIP before storing.
     */
    public void zput(String key, Object value, long ttl, TimeUnit unit) {
        try {
            String json = objectMapper.writeValueAsString(value);
            byte[] compressed = compress(json);
            redisTemplate.opsForValue().set(key, compressed, ttl, unit);
        } catch (Exception e) {
            log.error("Error in zput for key: {}", key, e);
        }
    }

    /**
     * Zipped Get: Decompresses the GZIP data from Redis.
     */
    public <T> T zget(String key, Class<T> type) {
        try {
            Object data = redisTemplate.opsForValue().get(key);
            if (!(data instanceof byte[])) return null;
            
            String json = decompress((byte[]) data);
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.error("Error in zget for key: {}", key, e);
            return null;
        }
    }

    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Failed to delete key: {}", key, e);
        }
    }

    /**
     * Delete all keys matching a specific pattern.
     * Useful for evicting related cache entries.
     */
    public void deleteKeysWithPattern(String pattern) {
        try {
            ScanOptions options = ScanOptions.scanOptions()
                    .match(pattern)
                    .count(1000)
                    .build();
            try (Cursor<String> cursor = redisTemplate.scan(options)) {
                int count = 0;
                while (cursor.hasNext()) {
                    String key = cursor.next();
                    try {
                        redisTemplate.delete(key);
                        count++;
                    } catch (Exception ex) {
                        log.warn("Failed to delete key {} during pattern deletion: {}", key, ex.getMessage());
                    }
                }
                log.debug("Deleted {} keys matching pattern: {}", count, pattern);
            }
        } catch (Exception e) {
            log.error("Failed to delete keys with pattern: {}", pattern, e);
        }
    }

    /**
     * Simple Put: Stores a plain string (No compression).
     * Best for short-lived codes like OTPs.
     */
    public void put(String key, String value, long ttl, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, ttl, unit);
    }

    /**
     * Simple Get: Retrieves a plain string.
     */
    public String get(String key) {
        Object val = redisTemplate.opsForValue().get(key);
        return val != null ? val.toString() : null;
    }

    private byte[] compress(String str) throws IOException {
        if (str == null || str.isEmpty()) return null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(str.getBytes(StandardCharsets.UTF_8));
        }
        return out.toByteArray();
    }

    private String decompress(byte[] bytes) throws IOException {
        if (bytes == null || bytes.length == 0) return null;
        try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
            return new String(gis.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
