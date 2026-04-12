package com.festora.menuservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Variant {
    private String id;
    private String label;
    private Double price;
    private Boolean available;
}
