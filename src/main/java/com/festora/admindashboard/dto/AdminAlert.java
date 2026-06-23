package com.festora.admindashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAlert {
    private String alertId;
    private String type;         // "DELAYED_ORDER", "LOW_STOCK", "CAPTAIN_CALL"
    private String message;
    private long timestamp;
    private String severity;     // "INFO", "WARNING", "CRITICAL"
}
