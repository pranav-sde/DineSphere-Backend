package com.festora.paymentservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "app.subscription")
@Data
public class SubscriptionPlanConfig {

    private Map<String, PlanDetails> plans = new HashMap<>();

    public boolean hasKitchenCaptainFlow(String planId) {
        return planId != null && (planId.startsWith("pro") || planId.startsWith("enterprise"));
    }

    public boolean hasBarInventory(String planId) {
        return planId != null && (planId.startsWith("pro") || planId.startsWith("enterprise"));
    }

    public boolean hasAdvancedAnalytics(String planId) {
        return planId != null && planId.startsWith("enterprise");
    }

    @Data
    public static class PlanDetails {
        private double price;
        private int months;

        public PlanDetails() {}

        public PlanDetails(double price, int months) {
            this.price = price;
            this.months = months;
        }
    }
}
