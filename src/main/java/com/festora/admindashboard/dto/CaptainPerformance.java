package com.festora.admindashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptainPerformance {
    private String captainId;
    private String captainName;
    private Long tablesServedCount;
    private Double averageServeTimeMinutes;
    private Long totalActionsCount;
}
