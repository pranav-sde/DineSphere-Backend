package com.festora.authservice.dto;

import com.festora.authservice.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserResponse {
    private String id;
    private String email;
    private UserRole role;
    private Long restaurantId;
    private boolean active;
}
