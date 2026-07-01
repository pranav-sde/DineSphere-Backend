package com.festora.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OwnerProfileResponse {

    private String id;
    private String email;
    private String ownerName;
    private String phoneNumber;
    private String restaurantName;
    private String address;
    private String fssaiLicense;
    private String gstNumber;
    private boolean enableDelivery;
    private String deliveryRadius;
    private String minOrderValue;
    private String latitude;
    private String longitude;
    private Long restaurantId;

    private String logoUrl;
    private Integer logoWidth;
    private Integer logoHeight;
}
