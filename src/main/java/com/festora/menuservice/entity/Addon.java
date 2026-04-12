package com.festora.menuservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class Addon {
    private String id;
    private String name;
    private Double price;
    private Boolean available;
}
