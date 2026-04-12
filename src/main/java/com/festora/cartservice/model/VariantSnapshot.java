package com.festora.cartservice.model;

import lombok.*;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    private String variantId;
    private String label;
}
