package com.festora.admindashboard.controller;

import com.festora.admindashboard.dto.*;
import com.festora.admindashboard.service.AdminDashboardService;
import com.festora.authservice.model.User;
import com.festora.authservice.repository.UserRepository;
import com.festora.paymentservice.config.SubscriptionPlanConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;
    private final UserRepository userRepository;
    private final SubscriptionPlanConfig subscriptionPlanConfig;

    private boolean checkKitchenFlow(Long restaurantId) {
        if (restaurantId == null) return false;
        String planId = userRepository.findByRestaurantId(restaurantId)
            .map(User::getSubscriptionPlan)
            .orElse("free");
        return subscriptionPlanConfig.hasKitchenCaptainFlow(planId);
    }

    private boolean checkAdvancedAnalytics(Long restaurantId) {
        if (restaurantId == null) return false;
        String planId = userRepository.findByRestaurantId(restaurantId)
            .map(User::getSubscriptionPlan)
            .orElse("free");
        return subscriptionPlanConfig.hasAdvancedAnalytics(planId);
    }

    // Live floor view — all 100 tables with color-coded status
    @GetMapping("/{restaurantId}/floor")
    public ResponseEntity<List<TableStatusView>> getLiveFloor(@PathVariable Long restaurantId) {
        if (!checkKitchenFlow(restaurantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            return ResponseEntity.ok(dashboardService.getLiveFloorView(restaurantId));
        } catch (Exception e) {
            log.error("Failed to retrieve live floor view: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // Live kitchen load per station
    @GetMapping("/{restaurantId}/kitchen-load")
    public ResponseEntity<Map<String, Integer>> getKitchenLoad(@PathVariable Long restaurantId) {
        if (!checkKitchenFlow(restaurantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            return ResponseEntity.ok(dashboardService.getKitchenLoad(restaurantId));
        } catch (Exception e) {
            log.error("Failed to retrieve kitchen load: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // Today's summary
    @GetMapping("/{restaurantId}/today")
    public ResponseEntity<DailyDashboardSummary> getTodaySummary(@PathVariable Long restaurantId) {
        try {
            return ResponseEntity.ok(dashboardService.getTodaySummary(restaurantId));
        } catch (Exception e) {
            log.error("Failed to retrieve today's summary: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // Active alerts (delayed orders, low stock, captain calls)
    @GetMapping("/{restaurantId}/alerts")
    public ResponseEntity<List<AdminAlert>> getAlerts(@PathVariable Long restaurantId) {
        if (!checkKitchenFlow(restaurantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            return ResponseEntity.ok(dashboardService.getActiveAlerts(restaurantId));
        } catch (Exception e) {
            log.error("Failed to retrieve active alerts: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // Captain performance report
    @GetMapping("/{restaurantId}/captain-report")
    public ResponseEntity<List<CaptainPerformance>> getCaptainReport(
            @PathVariable Long restaurantId,
            @RequestParam(required = false) Long from,
            @RequestParam(required = false) Long to) {
        if (!checkAdvancedAnalytics(restaurantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            long fromTime = from != null ? from : System.currentTimeMillis() - (24 * 60 * 60 * 1000L); // default 24h
            long toTime = to != null ? to : System.currentTimeMillis();
            return ResponseEntity.ok(dashboardService.getCaptainPerformance(restaurantId, fromTime, toTime));
        } catch (Exception e) {
            log.error("Failed to retrieve captain report: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
