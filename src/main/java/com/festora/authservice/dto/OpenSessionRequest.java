package com.festora.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OpenSessionRequest {
    @NotBlank
    private String qrToken;
}
