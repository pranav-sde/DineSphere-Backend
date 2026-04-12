package com.festora.inventoryservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.festora.inventoryservice.dto.InventoryReserveRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryMessageConsumer {

    private final RedisTemplate<String, Object> redisTemplate;
    private final InventoryService inventoryService;
    private final InventoryEventProducer eventProducer;
    private final ObjectMapper objectMapper;
    private static final String INVENTORY_COMMAND_QUEUE = "inventory:commands";

    @Scheduled(fixedDelay = 500)
    public void consumeInventoryCommands() {
        try {
            Object raw = redisTemplate.opsForList().leftPop(INVENTORY_COMMAND_QUEUE, 1, TimeUnit.SECONDS);
            
            if (raw != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> command = (Map<String, Object>) raw;
                String type = (String) command.get("type");
                Object data = command.get("data");

                log.info("Processing inventory command: {}", type);

                if ("TEMP_RESERVE".equals(type)) {
                    InventoryReserveRequest request = objectMapper.convertValue(data, InventoryReserveRequest.class);
                    try {
                        inventoryService.tempReserve(request);
                        eventProducer.notifyStatus(request.getOrderId(), "TEMP_RESERVED");
                    } catch (Exception e) {
                        log.error("Reservation failed for order {}: {}", request.getOrderId(), e.getMessage());
                        eventProducer.notifyStatus(request.getOrderId(), "OUT_OF_STOCK");
                    }
                } else if ("CONFIRM".equals(type)) {
                    String orderId = (String) data;
                    inventoryService.confirmReservation(orderId);
                }
                
                log.info("Finished processing inventory command: {}", type);
            }
        } catch (Exception e) {
            log.error("Error processing inventory command from Redis: {}", e.getMessage(), e);
        }
    }
}
