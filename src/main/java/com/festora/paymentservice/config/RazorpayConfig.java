package com.festora.paymentservice.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RazorpayConfig {

    @Value("${razorpay.key-id:}")
    private String keyId;

    @Value("${razorpay.key-secret:}")
    private String keySecret;

    @Bean
    @ConditionalOnProperty(name = "razorpay.key-id", matchIfMissing = false)
    public RazorpayClient razorpayClient() throws RazorpayException {
        if (keyId == null || keyId.isBlank()) {
            log.warn("Razorpay key-id is not configured. RazorpayClient bean will not be created.");
            return null;
        }
        return new RazorpayClient(keyId, keySecret);
    }

    public String getKeyId() {
        return keyId;
    }

    public String getKeySecret() {
        return keySecret;
    }
}
