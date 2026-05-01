package com.festora.orderservice.scheduler;

import com.festora.orderservice.enums.OrderStatus;
import com.festora.orderservice.model.Order;
import com.festora.orderservice.repository.OrderRepository;
import com.festora.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
@Component
@RequiredArgsConstructor
public class OrderExpiryScheduler {

    private static final long EXPIRY_WINDOW =  2 * 60 * 60 * 1000L; // 2 hours

    private final OrderRepository orderRepo;
    private final OrderService orderService;

    @Scheduled(fixedDelay = 120_000)
    public void expireOrders() {

        long cutoff = System.currentTimeMillis() - EXPIRY_WINDOW;

        // Only expire stuck orders that never reached the kitchen
        List<Order> expiredOrders =
                orderRepo.findByStatusInAndUpdatedAtBefore(
                        List.of(
                                OrderStatus.CREATED,
                                OrderStatus.PENDING
                        ),
                        cutoff
                );

        for (Order order : expiredOrders) {
            orderService.cancelOrder(
                    order.getOrderId(),
                    "SESSION_EXPIRED"
            );
        }
    }
}