package com.festora.authservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import lombok.Data;

@Data
@Document(collection = "qr_table_mapping")
@CompoundIndex(name = "restaurant_table_idx", def = "{'restaurantId': 1, 'tableNumber': 1}", unique = true)
public class QrTableMapping {

    @Id
    private String id;

    @Indexed(unique = true)
    private String qrId;

    @Field("restaurant_id")
    private Long restaurantId;

    @Field("table_number")
    private Integer tableNumber;

    private Boolean active = true;
}