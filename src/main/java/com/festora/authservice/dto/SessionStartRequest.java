package com.festora.authservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionStartRequest {
    private String qrId;
    private String deviceId; // optional: sent by frontend to allow session resumption
}

