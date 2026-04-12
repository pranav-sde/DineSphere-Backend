package com.festora.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GstResult {
    private double cgst;
    private double sgst;
    private double igst;
    private double totalTax;
}

