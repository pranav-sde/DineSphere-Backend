package com.festora.orderservice.client;

import com.festora.orderservice.model.Order;
import com.festora.orderservice.model.OrderItem;

import java.util.List;

public interface InventoryClient {

    void tempReserve(Order order);
    void tempReserve(Order order, List<OrderItem> item);
    void confirm(String orderId);
}

