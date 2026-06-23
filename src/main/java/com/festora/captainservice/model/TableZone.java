package com.festora.captainservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "table_zones")
public class TableZone {
    @Id 
    private String id;
    private Long restaurantId;
    private String zoneName;              // "Zone A", "Floor 1", "Terrace"
    private List<Integer> tableNumbers;   // [1, 2, 3, 4, 5]
    private String assignedCaptainId;     // User ID of captain
    private String assignedCaptainName;
    private boolean active;
    private long createdAt;
    private long updatedAt;
}
