package com.festora.orderservice.controller;

import com.festora.orderservice.scheduler.OrderCleanupScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/maintenance")
@RequiredArgsConstructor
public class SystemMaintenanceController {

    private final OrderCleanupScheduler orderCleanupScheduler;

    @org.springframework.beans.factory.annotation.Value("${app.maintenance.secret}")
    private String maintenanceSecret;

    @PostMapping("/cleanup-orders")
    public org.springframework.http.ResponseEntity<String> triggerCleanup(
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-Maintenance-Secret", required = false) String secret) {
        
        if (secret == null || !secret.equals(maintenanceSecret)) {
            return org.springframework.http.ResponseEntity
                    .status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body("Forbidden: Invalid maintenance secret");
        }
        
        orderCleanupScheduler.deleteRejectedOrCancelledOrders();
        return org.springframework.http.ResponseEntity.ok("Cleanup triggered successfully");
    }
}
