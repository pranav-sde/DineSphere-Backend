package com.festora.authservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import com.festora.orderservice.enums.SeatingType;
import lombok.Data;

@Data
@Document(collection = "qr_table_mapping")
@CompoundIndexes({
    @CompoundIndex(name = "restaurant_table_idx", def = "{'restaurantId': 1, 'tableNumber': 1, 'seatingType': 1}", unique = true)
})
public class QrTableMapping {

    @Id
    private String id;

    @Indexed(unique = true)
    private String qrId;

    @Field("restaurant_id")
    private Long restaurantId;

    @Field("table_number")
    private Integer tableNumber;

    @Field("seating_type")
    private SeatingType seatingType = SeatingType.TABLE;

    private Boolean active = true;
}