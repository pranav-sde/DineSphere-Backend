package com.festora.cartservice.client;

import com.festora.orderservice.service.OrderService;
import com.festora.cartservice.dto.client.OrderCreateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Monolith bridge for Order Client in Cart Service.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderClient {

    private final OrderService orderService;

    public Object createOrder(OrderCreateRequest request) {
        log.info("Directly calling OrderService.createOrder for session: {}", request.getUserId());

        // Map to OrderService DTO
        com.festora.orderservice.dto.CreateOrderRequest orderReq = new com.festora.orderservice.dto.CreateOrderRequest();
        orderReq.setRestaurantId(request.getRestaurantId());
        orderReq.setTableNumber(request.getTableNumber());
        orderReq.setUserId(request.getUserId());
        orderReq.setDeviceId(request.getDeviceId());
        
        orderReq.setItems(request.getItems().stream().map(i -> {
            com.festora.orderservice.model.OrderItem item = new com.festora.orderservice.model.OrderItem();
            item.setMenuItemId(i.getMenuItemId());
            item.setVariantId(i.getVariantId());
            item.setQuantity(i.getQuantity());
            item.setAddonIds(i.getAddonIds());
            return item;
        }).collect(Collectors.toList()));

        // Call service logic
        return orderService.createOrder(orderReq);
    }
}