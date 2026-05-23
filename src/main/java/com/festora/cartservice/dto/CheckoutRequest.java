package com.festora.cartservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.festora.orderservice.enums.SeatingType;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CheckoutRequest {
    private Long restaurantId;
    private Integer tableNumber;
    private String userId;
    private String userName;
    private String deviceId;
    private SeatingType seatingType;
    private String mobileNumber;
    private String roomNumber;
    private String hotelConfigId;
}