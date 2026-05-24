package com.festora.notificationservice.listener;

import com.festora.notificationservice.service.NotificationDispatcher;
import com.festora.orderservice.dto.event.OrderNotificationEvent;
import com.festora.orderservice.model.Order;
import com.festora.orderservice.model.OrderItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderNotificationEventListener {

    private final NotificationDispatcher dispatcher;

    @Async
    @EventListener
    public void handleOrderNotificationEvent(OrderNotificationEvent event) {
        try {
            Order order = event.getOrder();
            String message = formatMessage(order, event.getAction());
            
            dispatcher.dispatch(order.getRestaurantId(), message);
        } catch (Exception e) {
            log.error("Failed to process order notification event", e);
        }
    }

    private String formatMessage(Order order, String action) {
        StringBuilder sb = new StringBuilder();
        
        switch (action) {
            case "CREATED":
                sb.append("\uD83C\uDF54 *New Order #").append(order.getOrderId()).append("*\n\n");
                break;
            case "PAID":
                sb.append("✅ *Order Paid #").append(order.getOrderId()).append("*\n\n");
                break;
            case "CANCELLED":
                sb.append("❌ *Order Cancelled #").append(order.getOrderId()).append("*\n\n");
                break;
            default:
                sb.append("ℹ️ *Order Update #").append(order.getOrderId()).append("*\n\n");
        }

        sb.append("\uD83D\uDC64 ").append(order.getUserName() != null ? order.getUserName() : "Guest").append("\n");
        if (order.getMobileNumber() != null) {
            sb.append("\uD83D\uDCDE ").append(order.getMobileNumber()).append("\n");
        }
        sb.append("\uD83D\uDCB0 ₹").append(order.getTotalAmount()).append("\n");
        if (order.getTableNumber() != null) {
            sb.append("\uD83D\uDCCD Table: ").append(order.getTableNumber()).append("\n");
        }

        sb.append("\n*Items:*\n");
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                sb.append("• ").append(item.getName())
                  .append(" x").append(item.getQuantity())
                  .append("\n");
            }
        }

        return sb.toString();
    }
}
