package com.festora.menuservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AddonSnapshot {
    private String addonId;
    private String name;
    private double price;
}

