package com.festora.barservice.model;

import com.festora.kitchenservice.enums.TicketStatus;
import com.festora.kitchenservice.model.TicketItem;
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
@Document(collection = "bar_tickets")
public class BarTicket {
    @Id 
    private String id;
    private String ticketId;
    private String orderId;
    private Long restaurantId;
    private Integer tableNumber;
    private boolean isLiquorOrder;    // true = goes to LIQUOR station (age-verified)
    private List<TicketItem> drinks;
    private TicketStatus status;
    private String bartenderId;
    private long createdAt;
    private long updatedAt;
    private long readyAt;
}
