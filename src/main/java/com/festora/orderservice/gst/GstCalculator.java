package com.festora.orderservice.gst;

import com.festora.orderservice.dto.GstResult;
import com.festora.orderservice.model.RestaurantTaxConfig;
import com.festora.orderservice.repository.RestaurantTaxConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GstCalculator {

    private final RestaurantTaxConfigRepository taxRepo;

    public GstResult calculate(Long restaurantId, double subtotal) {

        RestaurantTaxConfig config = taxRepo
                .findByRestaurantId(restaurantId)
                .orElseThrow(() ->
                        new IllegalStateException("GST config missing for restaurant")
                );

        if (!config.isGstEnabled()) {
            return GstResult.builder()
                    .cgst(0)
                    .sgst(0)
                    .igst(0)
                    .totalTax(0)
                    .build();
        }

        // SAME STATE GST ONLY
        double cgst = subtotal * config.getCgstPercent() / 100;
        double sgst = subtotal * config.getSgstPercent() / 100;

        return GstResult.builder()
                .cgst(cgst)
                .sgst(sgst)
                .igst(0)              // explicitly zero
                .totalTax(cgst + sgst)
                .build();
    }
}