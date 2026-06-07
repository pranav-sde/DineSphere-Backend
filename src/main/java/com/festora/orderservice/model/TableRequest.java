package com.festora.orderservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "table_requests")
public class TableRequest {
    @Id
    private String id;
    
    @Indexed
    private Long restaurantId;
    
    private Integer tableNumber;
    private String userId;
    private String type; // CALL_WAITER, WATER, BILL
    private String status; // PENDING, RESOLVED
    private long createdAt;
    private long updatedAt;
}
