package com.festora.monolith.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.Callable;

@Configuration
@EnableCaching
@Slf4j
public class MonolithRedisConfig {

    @Bean
    @Primary
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper redisObjectMapper
    ) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 1. Setup Redis (L2) Cache Manager
        // Use an isolated serializer that doesn't affect global Jackson settings
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
        RedisCacheConfiguration redisConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(24)) // Long life in Redis
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();

        RedisCacheManager redisCacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(redisConfig)
                .build();

        // 2. Setup RAM (L1) Cache Manager
        ConcurrentMapCacheManager ramCacheManager = new ConcurrentMapCacheManager();

        // 3. Return Layered Manager
        return new LayeredCacheManager(ramCacheManager, redisCacheManager);
    }

    /**
     * Custom CacheManager that delegates to L1 (RAM) and L2 (Redis).
     */
    public static class LayeredCacheManager implements CacheManager {
        private final CacheManager l1;
        private final CacheManager l2;

        public LayeredCacheManager(CacheManager l1, CacheManager l2) {
            this.l1 = l1;
            this.l2 = l2;
        }

        @Override
        public Cache getCache(String name) {
            Cache c1 = l1.getCache(name);
            Cache c2 = l2.getCache(name);
            return new LayeredCache(c1, c2);
        }

        @Override
        public Collection<String> getCacheNames() {
            return l1.getCacheNames();
        }
    }

    /**
     * Custom Cache implementation: Check L1 -> Check L2 -> Populate L1.
     */
    public static class LayeredCache implements Cache {
        private final Cache l1;
        private final Cache l2;

        public LayeredCache(Cache l1, Cache l2) {
            this.l1 = l1;
            this.l2 = l2;
        }

        @Override
        public String getName() { return l1.getName(); }

        @Override
        public Object getNativeCache() { return l1.getNativeCache(); }

        @Override
        public ValueWrapper get(Object key) {
            try {
                // 1. Check RAM
                ValueWrapper wrapper = l1.get(key);
                if (wrapper != null) return wrapper;

                // 2. Check Redis
                wrapper = l2.get(key);
                if (wrapper != null) {
                    l1.put(key, wrapper.get()); // Populate RAM for next time
                }
                return wrapper;
            } catch (Exception e) {
                log.warn("Cache access error for key {}: {}. Evicting corrupted key.", key, e.getMessage());
                try { l2.evict(key); } catch (Exception ignore) {} 
                return null;
            }
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            try {
                T value = l1.get(key, type);
                if (value != null) return value;

                value = l2.get(key, type);
                if (value != null) {
                    l1.put(key, value);
                }
                return value;
            } catch (Exception e) {
                log.warn("Cache type mismatch for key {}: {}. Evicting corrupted key.", key, e.getMessage());
                try { l2.evict(key); } catch (Exception ignore) {}
                return null;
            }
        }

        @Override
        public <T> T get(Object key, Callable<T> valueLoader) {
            try {
                return l1.get(key, () -> {
                    T value = l2.get(key, valueLoader);
                    return value;
                });
            } catch (Exception e) {
                log.warn("Cache loader error for key {}: {}. Evicting corrupted key.", key, e.getMessage());
                try { l2.evict(key); } catch (Exception ignore) {}
                try {
                    return valueLoader.call();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

        @Override
        public void put(Object key, Object value) {
            l1.put(key, value);
            l2.put(key, value);
        }

        @Override
        public void evict(Object key) {
            l1.evict(key);
            l2.evict(key);
        }

        @Override
        public void clear() {
            l1.clear();
            l2.clear();
        }
    }
}
