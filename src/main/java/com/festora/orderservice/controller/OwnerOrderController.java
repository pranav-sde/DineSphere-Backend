package com.festora.orderservice.controller;

import com.festora.orderservice.dto.UpdateOrderItemsRequest;
import com.festora.orderservice.model.Order;
import com.festora.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/owner")
@RequiredArgsConstructor
public class OwnerOrderController {

    private final OrderService orderService;

    @GetMapping("/all")
    public ResponseEntity<List<Order>> getOrders(@RequestHeader("X-Restaurant-Id") Long restaurantId) {
        return ResponseEntity.ok(orderService.getActiveOwnerOrders(restaurantId));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(orderService.getOrder(orderId));
    }

    @PostMapping("/{orderId}/items")
    public ResponseEntity<Order> updateItems(
            @PathVariable String orderId,
            @RequestBody UpdateOrderItemsRequest request
    ) {
        return ResponseEntity.ok(
                orderService.updateOrderItems(orderId, request)
        );
    }

    @PostMapping("/{orderId}/finalize")
    public ResponseEntity<Order> finalizeOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(
                orderService.finalizeOrder(orderId)
        );
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable String orderId,
            @RequestParam String reason
    ) {
        orderService.cancelOrder(orderId, reason);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/todays")
    public ResponseEntity<List<Order>> getTodayOrders(@RequestHeader("X-Restaurant-Id") Long restaurantId){
        try{
            List<Order> todayOrders =  orderService.fetchTodaysAllOrders(restaurantId);
            return ResponseEntity.ok(todayOrders);
        } catch (Exception e){
          return ResponseEntity.badRequest().build();
        }
    }
}