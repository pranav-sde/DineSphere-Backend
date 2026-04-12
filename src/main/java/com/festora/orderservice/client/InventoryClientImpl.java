package com.festora.orderservice.client;

import com.festora.inventoryservice.service.InventoryService;
import com.festora.orderservice.model.Order;
import com.festora.orderservice.model.OrderItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Monolith implementation of InventoryClient that calls InventoryService directly.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryClientImpl implements InventoryClient {

    private final InventoryService inventoryService;
    private static final int RESERVE_TTL_SECONDS = 2 * 60 * 60; // 2 hours

    @Override
    public void tempReserve(Order order) {
        log.info("Directly calling InventoryService.tempReserve for order: {}", order.getOrderId());
        inventoryService.tempReserve(mapRequest(order));
    }

    @Override
    public void tempReserve(Order order, List<OrderItem> newItems) {
        log.info("Directly calling InventoryService.tempReserve for new items in order: {}", order.getOrderId());
        // For simplicity, we just reserve everything again or as configured in the microservice earlier
        inventoryService.tempReserve(mapRequest(order));
    }

    @Override
    public void confirm(String orderId) {
        log.info("Directly calling InventoryService.confirm Reservation for order: {}", orderId);
        inventoryService.confirmReservation(orderId);
    }

    private com.festora.inventoryservice.dto.InventoryReserveRequest mapRequest(Order order) {
        com.festora.inventoryservice.dto.InventoryReserveRequest request = new com.festora.inventoryservice.dto.InventoryReserveRequest();
        request.setOrderId(order.getOrderId());
        request.setRestaurantId(order.getRestaurantId());
        request.setTtlSeconds(RESERVE_TTL_SECONDS);
        
        List<com.festora.inventoryservice.dto.ReservedItemRequest> items = order.getItems().stream().map(i -> {
            com.festora.inventoryservice.dto.ReservedItemRequest item = new com.festora.inventoryservice.dto.ReservedItemRequest();
            item.setMenuItemId(i.getMenuItemId());
            item.setVariantId(i.getVariantId());
            item.setQuantity(i.getQuantity());
            return item;
        }).collect(Collectors.toList());
        
        request.setItems(items);
        return request;
    }
}