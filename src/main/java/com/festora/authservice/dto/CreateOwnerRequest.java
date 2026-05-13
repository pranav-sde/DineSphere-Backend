package com.festora.authservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateOwnerRequest {

    @Email
    @NotBlank
    private String businessEmail;

    @NotBlank
    private String password;

    private String ownerName;
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
}