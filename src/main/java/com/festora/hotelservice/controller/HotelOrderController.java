package com.festora.hotelservice.controller;

import com.festora.hotelservice.dto.CreateHotelOrderRequest;
import com.festora.hotelservice.service.HotelOrderService;
import com.festora.orderservice.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Handles hotel room-service order placement.
 * Guest identity is established by mobile number + room number (self-declared, no verification).
 */
@RestController
@RequestMapping("/order/hotel")
@RequiredArgsConstructor
@Slf4j
public class HotelOrderController {

    private final HotelOrderService hotelOrderService;

    /**
     * Guest confirms mobile + room, places order.
     * Room validation (if enabled) is checked before creating the order.
     */
    @PostMapping("/create")
    public ResponseEntity<?> createHotelOrder(@RequestBody CreateHotelOrderRequest request) {
        try {
            Order order = hotelOrderService.createHotelOrder(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    Map.of(
                            "orderId", order.getOrderId(),
                            "status", order.getStatus(),
                            "totalAmount", order.getTotalAmount(),
                            "hotelName", order.getHotelName() != null ? order.getHotelName() : "",
                            "roomNumber", order.getRoomNumber() != null ? order.getRoomNumber() : "",
                            "paymentMode", order.getPaymentMode()
                    )
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Hotel order creation failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Order creation failed"));
        }
    }

    /**
     * Guest retrieves their active orders by mobile + hotelConfigId.
     * Matches existing "my orders" pattern from the table flow.
     */
    @GetMapping("/my")
    public ResponseEntity<?> getMyHotelOrders(
            @RequestParam String hotelConfigId,
            @RequestParam String mobileNumber) {
        try {
            List<Order> orders = hotelOrderService.getOrdersByMobileAndHotel(mobileNumber, hotelConfigId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
