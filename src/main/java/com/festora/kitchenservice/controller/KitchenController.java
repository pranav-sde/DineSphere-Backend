package com.festora.kitchenservice.controller;

import com.festora.kitchenservice.enums.KitchenStation;
import com.festora.kitchenservice.enums.TicketStatus;
import com.festora.kitchenservice.model.KitchenTicket;
import com.festora.kitchenservice.service.KitchenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/kitchen")
@RequiredArgsConstructor
@Slf4j
public class KitchenController {

    private final KitchenService kitchenService;

    @GetMapping("/{restaurantId}/tickets")
    public ResponseEntity<List<KitchenTicket>> getTickets(
            @PathVariable Long restaurantId,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) KitchenStation station
    ) {
        try {
            List<KitchenTicket> tickets = kitchenService.getTickets(restaurantId, status, station);
            return ResponseEntity.ok(tickets);
        } catch (Exception e) {
            log.error("Failed to fetch kitchen tickets: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/tickets/{ticketId}/start")
    public ResponseEntity<Void> startTicket(
            @PathVariable String ticketId,
            @RequestHeader("X-Staff-Id") String staffId
    ) {
        try {
            kitchenService.markTicketInProgress(ticketId, staffId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to start kitchen ticket {}: {}", ticketId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/tickets/{ticketId}/ready")
    public ResponseEntity<Void> readyTicket(@PathVariable String ticketId) {
        try {
            kitchenService.markTicketReady(ticketId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to mark kitchen ticket ready {}: {}", ticketId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{restaurantId}/stats")
    public ResponseEntity<Map<String, Object>> getStats(
            @PathVariable Long restaurantId,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime
    ) {
        try {
            long start = startTime != null ? startTime : System.currentTimeMillis() - (24 * 60 * 60 * 1000L); // default 24h
            long end = endTime != null ? endTime : System.currentTimeMillis();
            Map<String, Object> stats = kitchenService.getKitchenStats(restaurantId, start, end);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Failed to fetch kitchen stats for restaurant {}: {}", restaurantId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
