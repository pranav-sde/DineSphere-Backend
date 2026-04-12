package com.festora.orderservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class UpdateOrderItemsRequest {
    private List<ItemUpdate> items;
}