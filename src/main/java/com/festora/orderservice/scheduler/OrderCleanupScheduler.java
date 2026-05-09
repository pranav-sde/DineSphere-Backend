package com.festora.orderservice.scheduler;

import com.festora.orderservice.enums.OrderStatus;
import com.festora.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCleanupScheduler {

    private final OrderRepository orderRepo;

    /**
     * Deletes REJECTED and CANCELLED orders every day at 3:00 AM.
     * Cron expression: "0 0 3 * * *" (Second, Minute, Hour, Day, Month, Weekday)
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void deleteRejectedOrCancelledOrders() {
        log.info("Starting scheduled cleanup of rejected and cancelled orders at 3 AM...");
        
        try {
            List<OrderStatus> statusesToDelete = List.of(OrderStatus.REJECTED, OrderStatus.CANCELLED);
            orderRepo.deleteByStatusIn(statusesToDelete);
            log.info("Successfully cleaned up rejected and cancelled orders.");
        } catch (Exception e) {
            log.error("Failed to perform scheduled order cleanup: {}", e.getMessage(), e);
        }
    }
}
