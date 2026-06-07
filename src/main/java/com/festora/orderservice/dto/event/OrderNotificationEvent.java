package com.festora.orderservice.dto.event;

import com.festora.orderservice.model.Order;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class OrderNotificationEvent extends ApplicationEvent {
    
    private final Order order;
    private final String action; // CREATED, PAID, CANCELLED

    public OrderNotificationEvent(Object source, Order order, String action) {
        super(source);
        this.order = order;
        this.action = action;
    }
}
