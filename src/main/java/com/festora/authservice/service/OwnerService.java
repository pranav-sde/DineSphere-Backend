package com.festora.authservice.service;

import com.festora.authservice.model.QrTableMapping;
import com.festora.authservice.repository.QrTableMappingRepository;
import com.festora.orderservice.enums.SeatingType;
import com.festora.subscription.config.PlanFeatures;
import com.festora.subscription.service.SubscriptionFeatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manages QR code generation for restaurant tables and private dining rooms.
 * Uses a unified QrTableMapping with seatingType to differentiate.
 */
@Service
@RequiredArgsConstructor
public class OwnerService {

    private final QrTableMappingRepository qrTableMappingRepository;
    private final SubscriptionFeatureService featureService;

    @Value("${app.frontend.url}")
    private String FRONTEND_QR_URL;

    // ── Table QR generation (existing flow) ─────────────────────────────

    public String getOrCreateMapping(Long restaurantId, Integer tableNumber) {
        return getOrCreateMapping(restaurantId, tableNumber, SeatingType.TABLE);
    }

    // ── Room QR generation (new flow) ───────────────────────────────────

    public String getOrCreateRoomMapping(Long restaurantId, Integer roomNumber) {
        return getOrCreateMapping(restaurantId, roomNumber, SeatingType.ROOM);
    }

    // ── Unified mapping creation with seatingType ───────────────────────

    private String getOrCreateMapping(Long restaurantId, Integer number, SeatingType seatingType) {
        if (restaurantId == null) {
            throw new IllegalArgumentException("restaurantId missing");
        }
        if (number == null || number <= 0) {
            throw new IllegalArgumentException("Invalid " + seatingType.name().toLowerCase() + " number");
        }

        QrTableMapping existing =
                qrTableMappingRepository.findByRestaurantIdAndTableNumberAndSeatingType(
                        restaurantId, number, seatingType);

        if (existing != null) {
            return buildQrUrl(existing.getQrId());
        }

        // Enforce subscription plan checks
        PlanFeatures features = featureService.getFeaturesForRestaurant(restaurantId);
        if (seatingType == SeatingType.ROOM && !features.isMultipleBranches()) {
            throw new IllegalArgumentException("Private Dining Room QR mapping is a Premium feature. Please upgrade your plan.");
        }

        int maxTables = features.getMaxTables();
        if (maxTables != -1) {
            long currentCount = qrTableMappingRepository.countByRestaurantId(restaurantId);
            if (currentCount >= maxTables) {
                throw new IllegalArgumentException("Maximum QR tables limit reached for this plan (" + maxTables + "). Please upgrade your plan.");
            }
        }

        QrTableMapping mapping = new QrTableMapping();
        mapping.setQrId(UUID.randomUUID().toString());
        mapping.setRestaurantId(restaurantId);
        mapping.setTableNumber(number);
        mapping.setSeatingType(seatingType);
        mapping.setActive(true);

        QrTableMapping saved = qrTableMappingRepository.save(mapping);
        return buildQrUrl(saved.getQrId());
    }

    // ── Bulk generation ─────────────────────────────────────────────────

    public List<String> generateUrlsInBulk(Long restaurantId, Integer start, Integer end) {
        return generateUrlsInBulk(restaurantId, start, end, SeatingType.TABLE);
    }

    public List<String> generateRoomUrlsInBulk(Long restaurantId, Integer start, Integer end) {
        return generateUrlsInBulk(restaurantId, start, end, SeatingType.ROOM);
    }

    private List<String> generateUrlsInBulk(Long restaurantId, Integer start, Integer end, SeatingType seatingType) {
        if (start == null || end == null || start <= 0 || end < start) {
            throw new IllegalArgumentException("Invalid range");
        }

        PlanFeatures features = featureService.getFeaturesForRestaurant(restaurantId);
        if (seatingType == SeatingType.ROOM && !features.isMultipleBranches()) {
            throw new IllegalArgumentException("Private Dining Room QR mapping is a Premium feature. Please upgrade your plan.");
        }

        int maxTables = features.getMaxTables();
        if (maxTables != -1) {
            long newCount = 0;
            for (int i = start; i <= end; i++) {
                QrTableMapping existing = qrTableMappingRepository.findByRestaurantIdAndTableNumberAndSeatingType(restaurantId, i, seatingType);
                if (existing == null) {
                    newCount++;
                }
            }
            if (newCount > 0) {
                long currentCount = qrTableMappingRepository.countByRestaurantId(restaurantId);
                if (currentCount + newCount > maxTables) {
                    throw new IllegalArgumentException("Generating " + newCount + " new QR codes would exceed your plan limit of " + maxTables + " tables (Current total: " + currentCount + "). Please upgrade your plan.");
                }
            }
        }

        List<String> urls = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            urls.add(getOrCreateMapping(restaurantId, i, seatingType));
        }
        return urls;
    }

    private String buildQrUrl(String qrId) {
        return FRONTEND_QR_URL +
                URLEncoder.encode(qrId, StandardCharsets.UTF_8);
    }
}