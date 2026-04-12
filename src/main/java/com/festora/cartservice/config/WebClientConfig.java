package com.festora.cartservice.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Production-grade WebClient configuration.
 *
 * WHY each setting matters:
 * - Connection pooling: Reuses TCP connections instead of creating new ones per request.
 *   On Render free tier, creating new connections is slow (~3-5s cold start).
 * - maxIdleTime(20s): Render kills idle connections after ~60s.
 *   We proactively close them at 20s to avoid "Connection reset by peer".
 * - maxLifeTime(60s): Forces connection recycling to prevent stale connections.
 * - evictInBackground(10s): Background thread scans and closes dead connections.
 * - connectTimeout(5s): Fail fast if a service is truly down, don't wait forever.
 * - readTimeout(15s): Individual read timeout per request.
 * - responseTimeout(15s): Total time to wait for the first byte of response.
 */
@Configuration
public class WebClientConfig {

    @Bean
    public ConnectionProvider connectionProvider() {
        return ConnectionProvider.builder("festora-pool")
                .maxConnections(50)                   // Max total connections
                .maxIdleTime(Duration.ofSeconds(20))   // Kill idle before Render does
                .maxLifeTime(Duration.ofSeconds(60))   // Recycle connections
                .evictInBackground(Duration.ofSeconds(10)) // Background cleanup
                .pendingAcquireTimeout(Duration.ofSeconds(10)) // Wait for available connection
                .build();
    }

    @Bean
    public WebClient.Builder webClientBuilder(ConnectionProvider connectionProvider) {
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000)  // 5s connect
                .responseTimeout(Duration.ofSeconds(15))               // 15s response
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(15, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS))
                );

        // Allow larger payloads (menu responses can be big)
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs()
                        .maxInMemorySize(2 * 1024 * 1024))  // 2MB
                .build();

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies);
    }

    @Bean
    public WebClient webClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder.build();
    }
}