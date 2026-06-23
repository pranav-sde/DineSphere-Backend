package com.festora.kitchenservice.model;

import com.festora.kitchenservice.enums.KitchenStation;
import com.festora.kitchenservice.enums.TicketStatus;
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
@Document(collection = "kitchen_tickets")
public class KitchenTicket {
    @Id 
    private String id;
    private String ticketId;          // UUID
    private String orderId;           // links to orders collection
    private Long restaurantId;
    private Integer tableNumber;
    private String roomNumber;        // for hotel orders
    private KitchenStation station;   // HOT / COLD / BAR / DESSERT / LIQUOR
    private List<TicketItem> items;   // only items for THIS station
    private TicketStatus status;      // OPEN / IN_PROGRESS / READY / PICKED_UP
    private String captainId;         // assigned captain for this table's zone
    private String assignedStaffId;   // who in kitchen accepted this ticket
    private String customerNote;      // any special instructions
    private long createdAt;
    private long updatedAt;
    private long readyAt;             // when kitchen marked ready
    private long pickedUpAt;          // when captain collected
}
