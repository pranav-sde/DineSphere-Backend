package com.festora.inventoryservice.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Legacy Kafka listener. 
 * Replaced by Redis-based InventoryMessageConsumer.
 */
@Component
@Slf4j
public class OrderCreatedListener {
    // Left empty until Kafka dependency is fully removed
}
