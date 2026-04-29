package com.festora.inventoryservice.service;

import com.festora.inventoryservice.dto.*;
import com.festora.inventoryservice.dto.event.InventoryReservationEvent;
import com.festora.inventoryservice.entity.*;
import com.festora.inventoryservice.enums.ReservationStatus;
import com.festora.inventoryservice.exception.OutOfStockException;
import com.festora.inventoryservice.repo.*;
import io.micrometer.common.util.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class InventoryService {

    private final InventoryStockRepository stockRepo;
    private final InventoryReservationRepository reservationRepo;
    private final InventoryReservationItemRepository reservationItemRepo;
    private final InventoryItemRepository inventoryItemRepo;

    /* =========================================================
       ORDER → INVENTORY FLOW
       ========================================================= */

    /**
     * Temp reserve: evicts inventory cache because reserved qty changes.
     */
    @CacheEvict(value = "ownerInventory", key = "#request.restaurantId")
    public InventoryReservationEvent tempReserve(InventoryReserveRequest request) {

        validateRequest(request);

        String orderId = request.getOrderId();

        // Idempotency
        InventoryReservation existing = reservationRepo.findByOrderId(orderId).orElse(null);
        if (existing != null) {
            return buildEvent(existing);
        }

        long now = System.currentTimeMillis();
        long ttl = request.getTtlSeconds() == 0 ? 300 : request.getTtlSeconds();
        long expiresAt = now + ttl * 1000;

        InventoryReservation reservation = new InventoryReservation();
        reservation.setOrderId(orderId);
        reservation.setStatus(ReservationStatus.TEMP_RESERVED);
        reservation.setCreatedAt(now);
        reservation.setExpiresAt(expiresAt);
        // Let MongoDB generate the ID
        reservation = reservationRepo.save(reservation);
        String reservationId = reservation.getReservationId();

        Map<String, Integer> requiredQtyByInvItemId = new HashMap<>();
        Map<String, InventoryItem> invItemMap = new HashMap<>();

        for (ReservedItemRequest reqItem : request.getItems()) {
            String variantId = reqItem.getVariantId();
            if (variantId != null && variantId.isBlank()) {
                variantId = null;
            }

            InventoryItem item = StringUtils.isBlank(variantId)
                    ? inventoryItemRepo.findAllByRestaurantIdAndMenuItemId(request.getRestaurantId(), reqItem.getMenuItemId()).stream().findFirst().orElse(null)
                    : inventoryItemRepo.findByRestaurantIdAndMenuItemIdAndVariantId(request.getRestaurantId(), reqItem.getMenuItemId(), variantId).orElse(null);

            if (item == null) {
                throw new OutOfStockException("ITEM_NOT_FOUND");
            }
            if (!item.isEnabled()) {
                throw new OutOfStockException("ITEM_DISABLED");
            }

            requiredQtyByInvItemId.merge(item.getId(), reqItem.getQuantity(), Integer::sum);
            invItemMap.put(item.getId(), item);
        }

        for (Map.Entry<String, Integer> entry : requiredQtyByInvItemId.entrySet()) {
            String invItemId = entry.getKey();
            int totalReqQty = entry.getValue();
            InventoryItem item = invItemMap.get(invItemId);

            InventoryStock stock = stockRepo.findById(invItemId).orElse(null);
            if (stock == null) {
                stock = new InventoryStock();
                stock.setInventoryItemId(invItemId);
                stock.setReservedQty(0);
                stock.setConfirmedQty(0);
                stock.setUpdatedAt(now);
            }

            int available =
                    item.getTotalStock()
                            - (stock.getReservedQty() + stock.getConfirmedQty());

            if (available < totalReqQty) {
                throw new OutOfStockException("INSUFFICIENT_STOCK");
            }

            stock.setReservedQty(stock.getReservedQty() + totalReqQty);
            stock.setUpdatedAt(now);
            stockRepo.save(stock);

            InventoryReservationItem ri = new InventoryReservationItem();
            ri.setReservationId(reservationId);
            ri.setInventoryItemId(invItemId);
            ri.setQuantity(totalReqQty);
            reservationItemRepo.save(ri);
        }

        return InventoryReservationEvent.builder()
                .orderId(orderId)
                .reservationId(reservationId)
                .restaurantId(request.getRestaurantId())
                .status(ReservationStatus.TEMP_RESERVED.name())
                .expiresAt(expiresAt)
                .build();
    }

    /**
     * Confirm: evicts because reserved→confirmed changes availability.
     * Note: We evict all entries because we don't have restaurantId in scope here.
     */
    @CacheEvict(value = "ownerInventory", allEntries = true)
    public void confirmReservation(String orderId) {

        InventoryReservation reservation = reservationRepo.findByOrderId(orderId).orElse(null);
        if (reservation == null || reservation.getStatus() == ReservationStatus.CONFIRMED) {
            return;
        }

        List<InventoryReservationItem> items =
                reservationItemRepo.findAllByReservationId(reservation.getReservationId());

        long now = System.currentTimeMillis();

        for (InventoryReservationItem item : items) {
            InventoryStock stock = stockRepo.findById(item.getInventoryItemId()).orElse(null);
            if (stock != null) {
                stock.setReservedQty(stock.getReservedQty() - item.getQuantity());
                stock.setConfirmedQty(stock.getConfirmedQty() + item.getQuantity());
                stock.setUpdatedAt(now);
                stockRepo.save(stock);
            }
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservationRepo.save(reservation);
    }

    @CacheEvict(value = "ownerInventory", allEntries = true)
    public void releaseByOrderId(String orderId) {

        InventoryReservation reservation = reservationRepo.findByOrderId(orderId).orElse(null);
        if (reservation == null ||
                reservation.getStatus() == ReservationStatus.RELEASED) {
            return;
        }

        List<InventoryReservationItem> items =
                reservationItemRepo.findAllByReservationId(reservation.getReservationId());

        long now = System.currentTimeMillis();

        for (InventoryReservationItem item : items) {
            InventoryStock stock = stockRepo.findById(item.getInventoryItemId()).orElse(null);
            if (stock != null) {
                stock.setReservedQty(stock.getReservedQty() - item.getQuantity());
                stock.setUpdatedAt(now);
                stockRepo.save(stock);
            }
        }

        reservation.setStatus(ReservationStatus.RELEASED);
        reservationRepo.save(reservation);
    }

    /* =========================================================
       OWNER → INVENTORY FLOW
       ========================================================= */

    /**
     * GET /all — cached by restaurantId.
     * WHY: Each call does findAllByRestaurantId + N stock lookups = N+1 DB queries.
     * The admin dashboard polls this frequently. With 15s TTL, most polls are cache hits.
     */
    @Cacheable(value = "ownerInventory", key = "#restaurantId")
    public List<OwnerInventoryResponse> getInventory(Long restaurantId) {
        log.info("Cache MISS: fetching inventory from DB for restaurant {}", restaurantId);
        return inventoryItemRepo.findAllByRestaurantId(restaurantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @CacheEvict(value = "ownerInventory", allEntries = true)
    public void updateTotalStock(UpdateStockRequest req) {

        InventoryItem item = inventoryItemRepo.findById(req.getInventoryItemId())
                .orElseThrow(() -> new IllegalArgumentException("Inventory item not found"));

        InventoryStock stock = stockRepo.findById(item.getId())
                .orElseThrow(() -> new IllegalStateException("Inventory stock missing"));

        if (req.getNewTotalStock() < stock.getConfirmedQty()) {
            throw new IllegalStateException("Cannot reduce stock below confirmed quantity");
        }

        item.setTotalStock(req.getNewTotalStock() + item.getTotalStock());
        item.setUpdatedAt(System.currentTimeMillis());
        inventoryItemRepo.save(item);
    }

    @Transactional
    @CacheEvict(value = "ownerInventory", allEntries = true)
    public void bulkUpsertStock(BulkUpdateStockRequest req, Long restaurantId) {
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new IllegalArgumentException("Items list cannot be empty");
        }

        long now = System.currentTimeMillis();

        // 1. Fetch all existing inventory items for this restaurant to avoid N queries in loop
        List<InventoryItem> allExistingItems = inventoryItemRepo.findAllByRestaurantId(restaurantId);
        
        // Map for quick lookup: menuItemId + ":" + (variantId != null ? variantId : "") -> InventoryItem
        Map<String, InventoryItem> existingItemsMap = new HashMap<>();
        for (InventoryItem item : allExistingItems) {
            String key = item.getMenuItemId() + ":" + (item.getVariantId() != null ? item.getVariantId() : "");
            existingItemsMap.put(key, item);
        }

        // Also map by ID for those that provide inventoryItemId
        Map<String, InventoryItem> itemsByIdMap = new HashMap<>();
        for (InventoryItem item : allExistingItems) {
            itemsByIdMap.put(item.getId(), item);
        }

        // 2. Fetch all stocks for these items
        List<String> allItemIds = allExistingItems.stream().map(InventoryItem::getId).toList();
        Map<String, InventoryStock> stockMap = new HashMap<>();
        stockRepo.findAllById(allItemIds).forEach(s -> stockMap.put(s.getInventoryItemId(), s));

        List<InventoryItem> itemsToSave = new ArrayList<>();
        int updateCount = 0;
        int insertCount = 0;

        for (BulkUpsertStockItem upsertReq : req.getItems()) {
            InventoryItem item = null;

            // Try to find by ID first
            if (upsertReq.getInventoryItemId() != null && !upsertReq.getInventoryItemId().isBlank()) {
                item = itemsByIdMap.get(upsertReq.getInventoryItemId());
            } 
            
            // If not found by ID (or ID not provided), try finding by menu item + variant
            if (item == null && upsertReq.getMenuItemId() != null && !upsertReq.getMenuItemId().isBlank()) {
                String variantId = upsertReq.getVariantId();
                if (variantId != null && variantId.isBlank()) variantId = null;
                String key = upsertReq.getMenuItemId() + ":" + (variantId != null ? variantId : "");
                item = existingItemsMap.get(key);
            }

            if (item != null) {
                // UPDATE existing
                InventoryStock stock = stockMap.get(item.getId());
                // In case stock is missing for some reason, we'll create it later but assume 0 for check
                int confirmedQty = (stock != null) ? stock.getConfirmedQty() : 0;

                if (upsertReq.getTotalStock() < confirmedQty) {
                    throw new IllegalStateException("Cannot reduce stock below confirmed quantity for item: " + item.getId());
                }

                item.setTotalStock(upsertReq.getTotalStock() + item.getTotalStock());
                item.setUpdatedAt(now);
                itemsToSave.add(item);
                updateCount++;
            } else {
                // INSERT new
                if (upsertReq.getMenuItemId() == null || upsertReq.getMenuItemId().isBlank()) {
                    throw new IllegalArgumentException("menuItemId is required for new inventory items");
                }

                InventoryItem newItem = new InventoryItem();
                newItem.setRestaurantId(restaurantId);
                newItem.setMenuItemId(upsertReq.getMenuItemId());
                String vId = upsertReq.getVariantId();
                if (vId != null && vId.isBlank()) vId = null;
                newItem.setVariantId(vId);
                newItem.setTotalStock(upsertReq.getTotalStock());
                newItem.setEnabled(upsertReq.isEnabled());
                newItem.setUpdatedAt(now);
                itemsToSave.add(newItem);
                insertCount++;
            }
        }

        // 3. Batch write all items
        List<InventoryItem> savedItems = inventoryItemRepo.saveAll(itemsToSave);

        // 4. Batch write all missing stocks
        List<InventoryStock> stocksToSave = new ArrayList<>();
        for (InventoryItem saved : savedItems) {
            if (!stockMap.containsKey(saved.getId())) {
                InventoryStock stock = new InventoryStock();
                stock.setInventoryItemId(saved.getId());
                stock.setReservedQty(0);
                stock.setConfirmedQty(0);
                stock.setUpdatedAt(now);
                stocksToSave.add(stock);
                // Update map so we don't try to add it again if the list had duplicates (though it shouldn't)
                stockMap.put(saved.getId(), stock);
            }
        }

        if (!stocksToSave.isEmpty()) {
            stockRepo.saveAll(stocksToSave);
        }

        log.info("Bulk upsert finished: {} total saved ({} updates, {} inserts)", savedItems.size(), updateCount, insertCount);
    }

    @CacheEvict(value = "ownerInventory", allEntries = true)
    public void toggleInventory(ToggleInventoryRequest req) {

        InventoryItem item = inventoryItemRepo.findById(req.getInventoryItemId())
                .orElseThrow(() -> new IllegalArgumentException("Inventory item not found"));

        item.setEnabled(req.isEnabled());
        item.setUpdatedAt(System.currentTimeMillis());
        inventoryItemRepo.save(item);
    }

    /* =========================================================
       HELPERS
       ========================================================= */

    private OwnerInventoryResponse toResponse(InventoryItem item) {

        InventoryStock stock = stockRepo.findById(item.getId())
                .orElseGet(() -> {
                    InventoryStock s = new InventoryStock();
                    s.setInventoryItemId(item.getId());
                    s.setReservedQty(0);
                    s.setConfirmedQty(0);
                    s.setUpdatedAt(System.currentTimeMillis());
                    return stockRepo.save(s);
                });

        int available =
                item.getTotalStock()
                        - (stock.getReservedQty() + stock.getConfirmedQty());

        return OwnerInventoryResponse.builder()
                .inventoryItemId(item.getId())
                .menuItemId(item.getMenuItemId())
                .variantId(item.getVariantId())
                .totalStock(item.getTotalStock())
                .reserved(stock.getReservedQty())
                .confirmed(stock.getConfirmedQty())
                .available(available)
                .enabled(item.isEnabled())
                .build();
    }

    private void validateRequest(InventoryReserveRequest request) {

        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        if (request.getOrderId() == null || request.getOrderId().isBlank()) {
            throw new IllegalArgumentException("orderId is required");
        }

        if (request.getRestaurantId() == null) {
            throw new IllegalArgumentException("restaurantId is required");
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("At least one item is required");
        }

        for (ReservedItemRequest item : request.getItems()) {
            if (item.getMenuItemId() == null || item.getMenuItemId().isBlank()) {
                throw new IllegalArgumentException("menuItemId is required");
            }
            if (item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than zero");
            }
        }
    }

    private InventoryReservationEvent buildEvent(InventoryReservation reservation) {

        return InventoryReservationEvent.builder()
                .orderId(reservation.getOrderId())
                .reservationId(reservation.getReservationId())
                .status(reservation.getStatus().name())
                .expiresAt(reservation.getExpiresAt())
                .build();
    }

    @Transactional
    @CacheEvict(value = "ownerInventory", key = "#req.restaurantId")
    public void createInventoryItem(CreateInventoryItemRequest req) {
        String variantId = req.getVariantId();
        if (variantId != null && variantId.isBlank()) {
            variantId = null;
        }
        // Check if inventory already exists
        InventoryItem item = StringUtils.isBlank(variantId)
                ? inventoryItemRepo.findAllByRestaurantIdAndMenuItemId(req.getRestaurantId(), req.getMenuItemId()).stream()
                    .filter(i -> i.getMenuItemId().equals(req.getMenuItemId()))
                    .findFirst().orElse(null)
                : inventoryItemRepo.findByRestaurantIdAndMenuItemIdAndVariantId(req.getRestaurantId(), req.getMenuItemId(), variantId).orElse(null);
        
        if (!ObjectUtils.isEmpty(item))
            throw new IllegalStateException("Inventory already exists");

        item = new InventoryItem();
        long now = System.currentTimeMillis();

        item.setRestaurantId(req.getRestaurantId());
        item.setMenuItemId(req.getMenuItemId());
        item.setVariantId(req.getVariantId());
        item.setTotalStock(req.getTotalStock());
        item.setEnabled(req.isEnabled());
        item.setUpdatedAt(now);

        // Let MongoDB generate the ID
        item = inventoryItemRepo.save(item);
        String inventoryItemId = item.getId();

        InventoryStock stock = new InventoryStock();
        stock.setInventoryItemId(inventoryItemId);
        stock.setReservedQty(0);
        stock.setConfirmedQty(0);
        stock.setUpdatedAt(now);

        stockRepo.save(stock);
    }
}