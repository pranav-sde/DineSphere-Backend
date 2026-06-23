package com.festora.barservice.controller;

import com.festora.barservice.model.BarInventoryItem;
import com.festora.barservice.model.BarTicket;
import com.festora.barservice.repository.BarTicketRepository;
import com.festora.barservice.service.BarService;
import com.festora.kitchenservice.enums.TicketStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bar")
@RequiredArgsConstructor
@Slf4j
public class BarController {

    private final BarService barService;
    private final BarTicketRepository barTicketRepository;

    // Bartender's display: open bar tickets
    @GetMapping("/{restaurantId}/tickets")
    public ResponseEntity<List<BarTicket>> getOpenTickets(@PathVariable Long restaurantId) {
        try {
            List<BarTicket> tickets = barTicketRepository.findByRestaurantIdAndStatusIn(
                    restaurantId, List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS));
            return ResponseEntity.ok(tickets);
        } catch (Exception e) {
            log.error("Failed to retrieve open bar tickets: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // Bartender marks drink ready
    @PutMapping("/tickets/{ticketId}/ready")
    public ResponseEntity<Void> markReady(@PathVariable String ticketId) {
        try {
            barService.markBarTicketReady(ticketId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to mark bar ticket ready {}: {}", ticketId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // Owner: view bar inventory
    @GetMapping("/{restaurantId}/inventory")
    public ResponseEntity<List<BarInventoryItem>> getInventory(@PathVariable Long restaurantId) {
        try {
            return ResponseEntity.ok(barService.getBarInventory(restaurantId));
        } catch (Exception e) {
            log.error("Failed to retrieve bar inventory for restaurant {}: {}", restaurantId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // Owner: add new bar item
    @PostMapping("/{restaurantId}/inventory")
    public ResponseEntity<BarInventoryItem> addBarItem(
            @PathVariable Long restaurantId,
            @RequestBody BarInventoryItem item) {
        try {
            item.setRestaurantId(restaurantId);
            return ResponseEntity.ok(barService.addBarItem(item));
        } catch (Exception e) {
            log.error("Failed to add bar item for restaurant {}: {}", restaurantId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // Owner: restock a bar item
    @PutMapping("/{restaurantId}/inventory/{itemId}/restock")
    public ResponseEntity<BarInventoryItem> restockItem(
            @PathVariable Long restaurantId,
            @PathVariable String itemId,
            @RequestParam double quantity) {
        try {
            return ResponseEntity.ok(barService.updateStock(restaurantId, itemId, quantity));
        } catch (Exception e) {
            log.error("Failed to restock item {}: {}", itemId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // Owner: low stock items only
    @GetMapping("/{restaurantId}/inventory/low-stock")
    public ResponseEntity<List<BarInventoryItem>> getLowStock(@PathVariable Long restaurantId) {
        try {
            return ResponseEntity.ok(barService.getLowStockItems(restaurantId));
        } catch (Exception e) {
            log.error("Failed to retrieve low stock items for restaurant {}: {}", restaurantId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
