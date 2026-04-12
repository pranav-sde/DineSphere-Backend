package com.festora.cartservice.model;

import lombok.*;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddonSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    private String addonId;
    private String name;
    private double price;
}
