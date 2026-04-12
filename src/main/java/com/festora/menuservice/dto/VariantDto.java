package com.festora.menuservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VariantDto {

    private String variantId;
    private String label;
    private Double price;
    private Boolean available;
}
