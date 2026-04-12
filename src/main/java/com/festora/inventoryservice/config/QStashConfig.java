package com.festora.inventoryservice.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;


public class QStashConfig {


    @Value("${qstash.token}")
    private String qstashToken;


    @Bean
    public WebClient qstashClient() {
        return WebClient.builder()
                .baseUrl("https://qstash.upstash.io/v1/publish")
                .defaultHeader("Authorization", "Bearer " + qstashToken)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}