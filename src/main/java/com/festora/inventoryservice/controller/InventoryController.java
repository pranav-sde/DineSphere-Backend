package com.festora.inventoryservice.controller;

import com.festora.inventoryservice.dto.*;
import com.festora.inventoryservice.dto.event.InventoryReservationEvent;
import com.festora.inventoryservice.exception.OutOfStockException;
import com.festora.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
@Slf4j
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return new ResponseEntity<>("Inventory Service up !!!", HttpStatus.OK);
    }

    @PostMapping("/temp-reserve")
    public ResponseEntity<InventoryReservationEvent> tempReserve(
            @RequestBody InventoryReserveRequest request
    ) {
        try {
            return ResponseEntity.ok(inventoryService.tempReserve(request));
        } catch (OutOfStockException ex) {
            log.warn("Inventory failure: {}", ex.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception ex) {
            log.error("Inventory error", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/confirm/{orderId}")
    public ResponseEntity<Void> confirm(@PathVariable String orderId) {
        inventoryService.confirmReservation(orderId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/all")
    public ResponseEntity<List<OwnerInventoryResponse>> getInventory(
            @RequestHeader("X-Restaurant-Id") Long restaurantId
    ) {
        return ResponseEntity.ok(inventoryService.getInventory(restaurantId));
    }

    @PatchMapping("/stock")
    public ResponseEntity<Void> updateStock(
            @RequestBody UpdateStockRequest req
    ) {
        inventoryService.updateTotalStock(req);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/toggle")
    public ResponseEntity<Void> toggle(
            @RequestBody ToggleInventoryRequest req
    ) {
        inventoryService.toggleInventory(req);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/item")
    public ResponseEntity<Void> createInventory(@RequestBody CreateInventoryItemRequest req, @RequestHeader("X-Restaurant-Id") Long restaurantId) {
        try {
            req.setRestaurantId(restaurantId);
            inventoryService.createInventoryItem(req);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}