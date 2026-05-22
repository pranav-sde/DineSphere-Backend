package com.festora.orderservice.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {
    private long totalOrders;
    private double todayRevenue;
    private int activeTables;
    private int activeRooms;
    private int activeHotelOrders;
    private long totalTables;
    private long totalRooms;
    private double avgOrderValue;
}
