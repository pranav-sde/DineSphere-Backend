package com.festora.orderservice.dto;

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
public class TableUserOrderGroup {
    private String userId;
    private String label;
    private List<Order> orders;
    private double totalAmount;
}