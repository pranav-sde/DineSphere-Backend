package com.festora.authservice.service;

import com.festora.authservice.model.QrTableMapping;
import com.festora.authservice.repository.QrTableMappingRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OwnerService {

    private final QrTableMappingRepository qrTableMappingRepository;
    @Value("${app.frontend.url}")
    private String FRONTEND_QR_URL;

    public String getOrCreateMapping(Long restaurantId, Integer tableNumber) {

        if (restaurantId == null) {
            throw new IllegalArgumentException("restaurantId missing");
        }

        if (tableNumber == null || tableNumber <= 0) {
            throw new IllegalArgumentException("Invalid table number");
        }

        QrTableMapping existing =
                qrTableMappingRepository
                        .findByRestaurantIdAndTableNumber(restaurantId, tableNumber);

        if (existing != null) {
            return buildQrUrl(existing.getQrId());
        }

        QrTableMapping mapping = new QrTableMapping();
        mapping.setQrId(UUID.randomUUID().toString());
        mapping.setRestaurantId(restaurantId);
        mapping.setTableNumber(tableNumber);
        mapping.setActive(true);

        QrTableMapping saved = qrTableMappingRepository.save(mapping);
        return buildQrUrl(saved.getQrId());
    }

    public List<String> generateUrlsInBulk(
            Long restaurantId,
            Integer start,
            Integer end
    ) {

        if (start == null || end == null || start <= 0 || end < start) {
            throw new IllegalArgumentException("Invalid table range");
        }

        List<String> urls = new ArrayList<>();

        for (int table = start; table <= end; table++) {
            urls.add(getOrCreateMapping(restaurantId, table));
        }

        return urls;
    }

    private String buildQrUrl(String qrId) {
        return FRONTEND_QR_URL +
                URLEncoder.encode(qrId, StandardCharsets.UTF_8);
    }
}