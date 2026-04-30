package com.festora.inventoryservice.service;

import com.festora.inventoryservice.entity.*;
import com.festora.inventoryservice.repo.*;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryTtlCleanupService {

    private final InventoryReservationRepository reservationRepo;
    private final InventoryReservationItemRepository reservationItemRepo;
    private final InventoryStockRepository stockRepo;
    private final CacheManager cacheManager;

    @Transactional
    public void cleanupExpiredReservations() {

        long now = System.currentTimeMillis();

        List<InventoryReservation> expiredReservations =
                reservationRepo.findAllByStatusAndExpiresAtBefore(
                        com.festora.inventoryservice.enums.ReservationStatus.TEMP_RESERVED,
                        now
                );

        if (expiredReservations.isEmpty()) return;

        Set<Long> restaurantsToEvict = new HashSet<>();

        for (InventoryReservation reservation : expiredReservations) {

            log.warn(
                    "TTL cleanup: releasing reservation {} (orderId={})",
                    reservation.getReservationId(),
                    reservation.getOrderId()
            );

            List<InventoryReservationItem> items =
                    reservationItemRepo.findAllByReservationId(
                            reservation.getReservationId()
                    );

            // Collect all inventory item IDs for this reservation to batch fetch stocks
            List<String> invItemIds = items.stream().map(InventoryReservationItem::getInventoryItemId).toList();
            Map<String, InventoryStock> stockMap = new HashMap<>();
            stockRepo.findAllById(invItemIds).forEach(s -> stockMap.put(s.getInventoryItemId(), s));

            for (InventoryReservationItem item : items) {
                InventoryStock stock = stockMap.get(item.getInventoryItemId());

                if (stock == null) {
                    log.error(
                            "InventoryStock missing for inventoryItemId={}",
                            item.getInventoryItemId()
                    );
                    continue;
                }

                int newReserved = stock.getReservedQty() - item.getQuantity();
                stock.setReservedQty(Math.max(newReserved, 0));
                stock.setUpdatedAt(now);
                stockRepo.save(stock);
            }

            reservation.setStatus(
                    com.festora.inventoryservice.enums.ReservationStatus.RELEASED
            );
            reservationRepo.save(reservation);
            
            if (reservation.getRestaurantId() != null) {
                restaurantsToEvict.add(reservation.getRestaurantId());
            }
        }

        // Batch evict caches
        for (Long rid : restaurantsToEvict) {
            evictCache(rid);
        }
    }

    private void evictCache(Long restaurantId) {
        if (restaurantId != null && cacheManager.getCache("ownerInventory") != null) {
            cacheManager.getCache("ownerInventory").evict(restaurantId);
        }
    }
}