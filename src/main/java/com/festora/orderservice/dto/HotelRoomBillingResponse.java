package com.festora.orderservice.dto;

import com.festora.orderservice.model.UserBill;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotelRoomBillingResponse {
    private String roomNumber;
    private List<TableUserOrderGroup> unbilledOrders;
    private List<UserBill> activeBills;
}
