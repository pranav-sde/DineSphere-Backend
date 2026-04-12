package com.festora.inventoryservice.service;

import com.festora.inventoryservice.entity.*;
import com.festora.inventoryservice.repo.*;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryTtlCleanupService {

    private final InventoryReservationRepository reservationRepo;
    private final InventoryReservationItemRepository reservationItemRepo;
    private final InventoryStockRepository stockRepo;

    @Transactional
    public void cleanupExpiredReservations() {

        long now = System.currentTimeMillis();

        List<InventoryReservation> expiredReservations =
                reservationRepo.findAllByStatusAndExpiresAtBefore(
                        com.festora.inventoryservice.enums.ReservationStatus.TEMP_RESERVED,
                        now
                );

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

            for (InventoryReservationItem item : items) {

                InventoryStock stock =
                        stockRepo.findById(item.getInventoryItemId()).orElse(null);

                if (stock == null) {
                    log.error(
                            "InventoryStock missing for inventoryItemId={}",
                            item.getInventoryItemId()
                    );
                    continue;
                }

                int newReserved =
                        stock.getReservedQty() - item.getQuantity();

                stock.setReservedQty(Math.max(newReserved, 0));
                stock.setUpdatedAt(now);
                stockRepo.save(stock);
            }

            reservation.setStatus(
                    com.festora.inventoryservice.enums.ReservationStatus.RELEASED
            );
            reservationRepo.save(reservation);
        }
    }
}