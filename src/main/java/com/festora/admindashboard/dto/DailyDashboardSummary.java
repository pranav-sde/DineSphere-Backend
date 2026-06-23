package com.festora.admindashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyDashboardSummary {
    private Double totalRevenue;
    private Long totalOrdersCount;
    private Double averageOrderValue;
    private Integer activeTablesCount;
    private Integer pendingKitchenTicketsCount;
}
