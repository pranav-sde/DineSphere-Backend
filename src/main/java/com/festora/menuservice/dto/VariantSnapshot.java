package com.festora.menuservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VariantSnapshot {
    private String variantId;
    private String label;
}