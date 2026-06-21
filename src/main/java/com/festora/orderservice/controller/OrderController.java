package com.festora.orderservice.controller;

import com.festora.orderservice.dto.CreateOrderRequest;
import com.festora.orderservice.dto.OrderCreateResponse;
import com.festora.orderservice.model.Order;
import com.festora.orderservice.model.OrderItem;
import com.festora.orderservice.service.OrderService;
import com.festora.orderservice.service.BillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final BillService billService;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return new ResponseEntity<>("Order Service up !!!", HttpStatus.OK);
    }
    
    @GetMapping("/my")
    public ResponseEntity<List<Order>> getMyOrders(
            @RequestHeader("X-Restaurant-Id") Long restaurantId,
            @RequestHeader("X-Table-No") Integer tableNumber,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            @RequestParam(value = "activeOnly", defaultValue = "false") boolean activeOnly
    ) {
        try {
            return new ResponseEntity<>(orderService.getAllOrdersForTableByRestaurantId(restaurantId, tableNumber, userId, deviceId, activeOnly), HttpStatus.OK);
        } catch (Exception e){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/my/bill")
    public ResponseEntity<com.festora.orderservice.model.UserBill> getMyBill(
            @RequestHeader("X-Restaurant-Id") Long restaurantId,
            @RequestHeader("X-User-Id") String userId
    ) {
        try {
            com.festora.orderservice.model.UserBill bill = 
                    billService.getMyLatestBill(userId, restaurantId);
            if (bill == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(bill);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/create")
    public ResponseEntity<OrderCreateResponse> createOrder(@RequestBody CreateOrderRequest request) {
        try {
            log.info("[ORDER_INITIATED] deviceId={}, restaurantId={}, tableNumber={}", request.getDeviceId(), request.getRestaurantId(), request.getTableNumber());
            Order order = orderService.createOrder(request);
            log.info("[ORDER_SUCCESS] orderId={}, deviceId={}, restaurantId={}, totalAmount={}", order.getOrderId(), request.getDeviceId(), request.getRestaurantId(), order.getTotalAmount());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(OrderCreateResponse.builder()
                            .orderId(order.getOrderId())
                            .status(order.getStatus())
                            .totalAmount(order.getTotalAmount())
                            .build());
        } catch (Exception e) {
            log.error("[ORDER_FAILED] deviceId={}, restaurantId={}, reason={}", request.getDeviceId(), request.getRestaurantId(), e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ==================================================
    // 2) Get Order (UI / Admin / Kitchen)
    // ==================================================
    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(orderService.getOrder(orderId));
    }



    @PostMapping("/{orderId}/payment-method/cod")
    public ResponseEntity<Void> selectCOD(@PathVariable String orderId) {
        orderService.selectCOD(orderId);
        return ResponseEntity.ok().build();
    }

//    @PostMapping("/{orderId}/ready")
//    public ResponseEntity<Void> markReady(@PathVariable String orderId) {
//        orderService.markReady(orderId);
//        return ResponseEntity.ok().build();
//    }

    @GetMapping("/{orderId}/served")
    public ResponseEntity<Void> markServed(@PathVariable String orderId) {
        orderService.markServed(orderId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{orderId}/close")
    public ResponseEntity<Void> closeOrder(@PathVariable String orderId) {
        orderService.closeOrder(orderId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{orderId}/items")
    public ResponseEntity<Order> addItems(
            @PathVariable String orderId,
            @RequestBody List<OrderItem> items
    ) {
        return ResponseEntity.ok(
                orderService.addItems(orderId, items)
        );
    }

    @PostMapping("/{orderId}/request-bill")
    public ResponseEntity<Void> requestBill(@PathVariable String orderId) {
        orderService.requestBill(orderId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable String orderId,
            @RequestParam(required = false) String reason
    ) {
        orderService.cancelOrder(orderId,
                reason == null ? "MANUAL_CANCEL" : reason);
        return ResponseEntity.ok().build();
    }

}