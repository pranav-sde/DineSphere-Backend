package com.festora.inventoryservice.scheduler;

import com.festora.inventoryservice.service.InventoryTtlCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class InventoryTtlCleanupScheduler {

    private final InventoryTtlCleanupService cleanupService;

    @Scheduled(fixedDelay = 60000) // every 1 min
    public void runTtlCleanup() {
        cleanupService.cleanupExpiredReservations();
    }
}
