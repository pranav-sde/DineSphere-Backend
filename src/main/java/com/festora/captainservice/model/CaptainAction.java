package com.festora.captainservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "captain_actions")
public class CaptainAction {
    @Id 
    private String id;
    private Long restaurantId;
    private String captainId;
    private String captainName;
    private Integer tableNumber;
    private String ticketId;
    private String actionType;    // "PICKED_UP", "SERVED", "CALLED", "BILL_REQUESTED"
    private long timestamp;
}
