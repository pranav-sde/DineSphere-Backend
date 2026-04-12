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

    private static final long EXPIRY_WINDOW =  3 * 60 * 60 * 1000L; // 3 hours

    private final OrderRepository orderRepo;
    private final OrderService orderService;

    @Scheduled(fixedDelay = 60_000)
    public void expireOrders() {

        long cutoff = System.currentTimeMillis() - EXPIRY_WINDOW;

        List<Order> expiredOrders =
                orderRepo.findByStatusInAndUpdatedAtBefore(
                        List.of(
                                OrderStatus.PAYMENT_PENDING,
                                OrderStatus.PREPARING
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