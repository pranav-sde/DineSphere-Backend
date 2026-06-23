package com.festora.barservice.model;

import com.festora.barservice.enums.BarStockUnit;
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
@Document(collection = "bar_inventory")
public class BarInventoryItem {
    @Id 
    private String id;
    private Long restaurantId;
    private String itemName;         // "Old Monk Rum", "Kingfisher Beer"
    private String brand;
    private String category;         // "Rum", "Beer", "Wine", "Whiskey", "Mixer"
    private BarStockUnit unit;       // BOTTLE / PINT / ML / UNIT
    private double totalStock;       // e.g. 24.0 bottles
    private double reservedStock;    // currently in use
    private double availableStock;   // totalStock - reservedStock
    private double lowStockThreshold; // alert if below this
    private boolean active;
    private long updatedAt;
}
