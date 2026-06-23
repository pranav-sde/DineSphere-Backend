package com.festora.monolith.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
public class MultiMongoConfig {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Bean
    @Primary
    public MongoClient mongoClient() {
        ServerApi serverApi = ServerApi.builder()
                .version(ServerApiVersion.V1)
                .build();

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(mongoUri))
                .serverApi(serverApi)
                .build();
        return MongoClients.create(settings);
    }

    // --- Menu Service Config ---
    @Configuration
    @EnableMongoRepositories(
            basePackages = "com.festora.menuservice.repository",
            mongoTemplateRef = "menuMongoTemplate"
    )
    public static class MenuMongoConfig {
        @Bean
        @Primary
        public MongoTemplate menuMongoTemplate(MongoClient mongoClient) {
            return new MongoTemplate(new SimpleMongoClientDatabaseFactory(mongoClient, "menu"));
        }
    }

    // --- Order Service Config ---
    @Configuration
    @EnableMongoRepositories(
            basePackages = {"com.festora.orderservice.repository", "com.festora.paymentservice.repository"},
            mongoTemplateRef = "orderMongoTemplate"
    )
    public static class OrderMongoConfig {
        @Bean
        public MongoTemplate orderMongoTemplate(MongoClient mongoClient) {
            return new MongoTemplate(new SimpleMongoClientDatabaseFactory(mongoClient, "order"));
        }
    }

    // --- Inventory Service Config ---
    @Configuration
    @EnableMongoRepositories(
            basePackages = "com.festora.inventoryservice.repo",
            mongoTemplateRef = "inventoryMongoTemplate"
    )
    public static class InventoryMongoConfig {
        @Bean
        public MongoTemplate inventoryMongoTemplate(MongoClient mongoClient) {
            return new MongoTemplate(new SimpleMongoClientDatabaseFactory(mongoClient, "inventory_db"));
        }
    }

    // --- Auth Service Config ---
    @Configuration
    @EnableMongoRepositories(
            basePackages = "com.festora.authservice.repository",
            mongoTemplateRef = "authMongoTemplate"
    )
    public static class AuthMongoConfig {
        @Bean
        public MongoTemplate authMongoTemplate(MongoClient mongoClient) {
            return new MongoTemplate(new SimpleMongoClientDatabaseFactory(mongoClient, "auth_db"));
        }
    }

    // --- Cart Service Config ---
    @Configuration
    @EnableMongoRepositories(
            basePackages = "com.festora.cartservice.repository",
            mongoTemplateRef = "cartMongoTemplate"
    )
    public static class CartMongoConfig {
        @Bean
        public MongoTemplate cartMongoTemplate(MongoClient mongoClient) {
            return new MongoTemplate(new SimpleMongoClientDatabaseFactory(mongoClient, "cart_db"));
        }
    }

    // --- Hotel Service Config ---
    @Configuration
    @EnableMongoRepositories(
            basePackages = "com.festora.hotelservice.repository",
            mongoTemplateRef = "hotelMongoTemplate"
    )
    public static class HotelMongoConfig {
        @Bean
        public MongoTemplate hotelMongoTemplate(MongoClient mongoClient) {
            return new MongoTemplate(new SimpleMongoClientDatabaseFactory(mongoClient, "hotel_db"));
        }
    }

    // --- Notification Service Config ---
    @Configuration
    @EnableMongoRepositories(
            basePackages = "com.festora.notificationservice.repository",
            mongoTemplateRef = "notificationMongoTemplate"
    )
    public static class NotificationMongoConfig {
        @Bean
        public MongoTemplate notificationMongoTemplate(MongoClient mongoClient) {
            return new MongoTemplate(new SimpleMongoClientDatabaseFactory(mongoClient, "notification_db"));
        }
    }

    // --- Kitchen Service Config ---
    @Configuration
    @EnableMongoRepositories(
            basePackages = "com.festora.kitchenservice.repository",
            mongoTemplateRef = "kitchenMongoTemplate"
    )
    public static class KitchenMongoConfig {
        @Bean
        public MongoTemplate kitchenMongoTemplate(MongoClient mongoClient) {
            return new MongoTemplate(new SimpleMongoClientDatabaseFactory(mongoClient, "kitchen_db"));
        }
    }

    // --- Captain Service Config ---
    @Configuration
    @EnableMongoRepositories(
            basePackages = "com.festora.captainservice.repository",
            mongoTemplateRef = "captainMongoTemplate"
    )
    public static class CaptainMongoConfig {
        @Bean
        public MongoTemplate captainMongoTemplate(MongoClient mongoClient) {
            return new MongoTemplate(new SimpleMongoClientDatabaseFactory(mongoClient, "captain_db"));
        }
    }

    // --- Bar Service Config ---
    @Configuration
    @EnableMongoRepositories(
            basePackages = "com.festora.barservice.repository",
            mongoTemplateRef = "barMongoTemplate"
    )
    public static class BarMongoConfig {
        @Bean
        public MongoTemplate barMongoTemplate(MongoClient mongoClient) {
            return new MongoTemplate(new SimpleMongoClientDatabaseFactory(mongoClient, "bar_db"));
        }
    }
}
