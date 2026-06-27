package com.festora.captainservice.controller;

import com.festora.authservice.model.User;
import com.festora.authservice.repository.UserRepository;
import com.festora.captainservice.dto.CaptainTableView;
import com.festora.captainservice.model.CaptainAction;
import com.festora.captainservice.model.TableZone;
import com.festora.captainservice.service.CaptainService;
import com.festora.kitchenservice.model.KitchenTicket;
import com.festora.kitchenservice.repository.KitchenTicketRepository;
import com.festora.paymentservice.config.SubscriptionPlanConfig;
import com.festora.orderservice.service.TableRequestService;
import com.festora.orderservice.repository.TableRequestRepository;
import com.festora.orderservice.model.TableRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/captain")
@RequiredArgsConstructor
@Slf4j
public class CaptainController {

    private final CaptainService captainService;
    private final UserRepository userRepository;
    private final SubscriptionPlanConfig subscriptionPlanConfig;
    private final KitchenTicketRepository ticketRepository;
    private final TableRequestService tableRequestService;
    private final TableRequestRepository tableRequestRepository;

    private boolean checkKitchenFlow(Long restaurantId) {
        if (restaurantId == null) return false;
        String planId = userRepository.findByRestaurantId(restaurantId)
            .map(User::getSubscriptionPlan)
            .orElse("free");
        return subscriptionPlanConfig.hasKitchenCaptainFlow(planId);
    }

    private boolean checkKitchenFlowForTicket(String ticketId) {
        if (ticketId == null) return false;
        Long restaurantId = ticketRepository.findByTicketId(ticketId)
            .map(KitchenTicket::getRestaurantId)
            .orElse(null);
        return checkKitchenFlow(restaurantId);
    }

    private boolean checkAdvancedAnalytics(Long restaurantId) {
        if (restaurantId == null) return false;
        String planId = userRepository.findByRestaurantId(restaurantId)
            .map(User::getSubscriptionPlan)
            .orElse("free");
        return subscriptionPlanConfig.hasAdvancedAnalytics(planId);
    }

    // Admin creates/updates zone assignment
    @PostMapping("/zones")
    public ResponseEntity<TableZone> createZone(@RequestBody TableZone zone) {
        if (zone != null && !checkKitchenFlow(zone.getRestaurantId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            return ResponseEntity.ok(captainService.createOrUpdateZone(zone));
        } catch (Exception e) {
            log.error("Failed to create/update zone: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // Admin views all zones for restaurant
    @GetMapping("/{restaurantId}/zones")
    public ResponseEntity<List<TableZone>> getZones(@PathVariable Long restaurantId) {
        if (!checkKitchenFlow(restaurantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            return ResponseEntity.ok(captainService.getZonesForRestaurant(restaurantId));
        } catch (Exception e) {
            log.error("Failed to get zones for restaurant {}: {}", restaurantId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // Captain's mobile live dashboard
    @GetMapping("/{restaurantId}/my-tables")
    public ResponseEntity<List<CaptainTableView>> getMyTables(
            @PathVariable Long restaurantId,
            @RequestHeader("X-User-Id") String captainId) {
        if (!checkKitchenFlow(restaurantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            return ResponseEntity.ok(captainService.getLiveTablesForCaptain(restaurantId, captainId));
        } catch (Exception e) {
            log.error("Failed to get live tables for captain {}: {}", captainId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // Captain marks ticket as served
    @PutMapping("/tickets/{ticketId}/served")
    public ResponseEntity<Void> markServed(@PathVariable String ticketId,
                                            @RequestHeader("X-User-Id") String captainId) {
        if (!checkKitchenFlowForTicket(ticketId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            captainService.markAsServed(ticketId, captainId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to mark ticket served {}: {}", ticketId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // Customer calls captain from QR screen (no auth needed, session token only)
    @PostMapping("/{restaurantId}/call")
    public ResponseEntity<Void> callCaptain(
            @PathVariable Long restaurantId,
            @RequestParam Integer tableNumber,
            @RequestParam(defaultValue = "ASSISTANCE") String requestType) {
        if (!checkKitchenFlow(restaurantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            captainService.notifyCaptain(restaurantId, tableNumber, requestType);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to notify captain for restaurant {} table {}: {}", restaurantId, tableNumber, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // Captain sends message back to customer table screen
    @PostMapping("/{restaurantId}/message-table")
    public ResponseEntity<Void> messageTable(
            @PathVariable Long restaurantId,
            @RequestParam Integer tableNumber,
            @RequestParam String message,
            @RequestHeader("X-User-Id") String captainId) {
        if (!checkKitchenFlow(restaurantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            captainService.sendMessageToTable(restaurantId, tableNumber, message);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to send captain message to table {}: {}", tableNumber, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // Captain's action history (admin can view)
    @GetMapping("/{restaurantId}/actions")
    public ResponseEntity<List<CaptainAction>> getCaptainActions(
            @PathVariable Long restaurantId,
            @RequestParam(required = false) String captainId,
            @RequestParam(required = false) Long from,
            @RequestParam(required = false) Long to) {
        if (!checkAdvancedAnalytics(restaurantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            long fromTime = from != null ? from : System.currentTimeMillis() - (24 * 60 * 60 * 1000L);
            long toTime = to != null ? to : System.currentTimeMillis();
            return ResponseEntity.ok(captainService.getActions(restaurantId, captainId, fromTime, toTime));
        } catch (Exception e) {
            log.error("Failed to get captain actions for restaurant {}: {}", restaurantId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // Captain resolves a customer request (assistance, water, etc.)
    @PutMapping("/requests/{requestId}/resolve")
    public ResponseEntity<TableRequest> resolveRequest(
            @PathVariable String requestId,
            @RequestHeader("X-User-Id") String captainId) {
        try {
            TableRequest request = tableRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
            if (!checkKitchenFlow(request.getRestaurantId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.ok(tableRequestService.resolveRequest(requestId));
        } catch (Exception e) {
            log.error("Failed to resolve table request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
