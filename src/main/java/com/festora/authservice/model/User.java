package com.festora.authservice.model;

import com.festora.authservice.enums.UserRole;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import lombok.*;

@Document(collection = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String passwordHash;

    private UserRole role;

    private boolean active;

    @Indexed(unique = true)
    @Field("restaurant_id")
    private Long restaurantId;

    private String ownerName;

    @Indexed(unique = true)
    private String phoneNumber;
    private String fssaiLicense;
    private String restaurantName;
    private String address;
    private String gstNumber;
    private boolean enableDelivery;
    private String deliveryRadius;
    private String minOrderValue;
    private String latitude;
    private String longitude;

    private String subscriptionPlan;
    private java.time.LocalDateTime subscriptionExpiry;
}