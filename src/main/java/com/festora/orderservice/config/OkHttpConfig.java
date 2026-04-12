package com.festora.orderservice.config;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Shared, production-grade OkHttpClient configuration.
 *
 * WHY this exists:
 * The old code created a bare `new OkHttpClient()` per client class.
 * That means:
 *  1. ZERO timeouts — hangs forever when downstream is cold-starting on Render
 *  2. No connection pooling — every request opens a new TCP connection (slow)
 *  3. Stale connections — connections idle for >60s get "Connection reset by peer"
 *     because Render's proxy kills them
 *
 * This shared bean:
 *  - Sets 5s connect timeout (fail fast if service truly down)
 *  - Sets 15s read timeout (generous for Render cold starts)
 *  - Sets 10s write timeout (POST bodies shouldn't take this long)
 *  - Reuses up to 10 connections with 30s idle timeout
 *  - Retries on connection failure (handles "connection reset by peer")
 */
@Configuration
public class OkHttpConfig {

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)     // Fail fast
                .readTimeout(15, TimeUnit.SECONDS)       // Generous for cold start
                .writeTimeout(10, TimeUnit.SECONDS)      // POST body timeout
                .connectionPool(new ConnectionPool(
                        10,                               // Max idle connections
                        30, TimeUnit.SECONDS              // Idle timeout (< Render's 60s kill)
                ))
                .retryOnConnectionFailure(true)           // Auto-retry "connection reset"
                .build();
    }
}
