package com.festora.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class ApiError {
    private String message;
    private int status;
    private Instant timestamp;
}