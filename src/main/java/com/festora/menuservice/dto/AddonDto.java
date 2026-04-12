package com.festora.menuservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddonDto {

    private String addonId;
    private String name;
    private Double price;
    private Boolean available;
}

