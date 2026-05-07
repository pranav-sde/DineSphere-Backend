package com.festora.authservice.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {

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
}
