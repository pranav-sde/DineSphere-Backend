package com.festora.captainservice.dto;

import com.festora.kitchenservice.model.KitchenTicket;
import com.festora.orderservice.model.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptainTableView {
    private Integer tableNumber;
    private List<Order> activeOrders;
    private List<KitchenTicket> pendingPickups;
}
