package com.festora.orderservice.gst.service;

import com.festora.orderservice.model.RestaurantTaxConfig;
import com.festora.orderservice.repository.RestaurantTaxConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RestaurantTaxConfigService {

    private final RestaurantTaxConfigRepository repository;

    public RestaurantTaxConfig saveOrUpdate(RestaurantTaxConfig config) {

        // if config already exists → update instead of duplicate
        return repository.findByRestaurantId(config.getRestaurantId())
                .map(existing -> {
                    existing.setGstEnabled(config.isGstEnabled());
                    existing.setCgstPercent(config.getCgstPercent());
                    existing.setSgstPercent(config.getSgstPercent());
                    existing.setIgstPercent(config.getIgstPercent());
                    existing.setRestaurantState(config.getRestaurantState());
                    return repository.save(existing);
                })
                .orElseGet(() -> repository.save(config));
    }

    public RestaurantTaxConfig getByRestaurantId(Long restaurantId) {
        return repository.findByRestaurantId(restaurantId)
                .orElseThrow(() -> new RuntimeException("GST config not found"));
    }
}