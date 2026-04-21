package com.festora.orderservice.service;

import com.festora.orderservice.client.InventoryClient;
import com.festora.orderservice.client.MenuClient;
import com.festora.orderservice.dto.*;
import com.festora.monolith.dto.MenuItemRedis;
import com.festora.orderservice.dto.event.InventoryConsumerEvent;
import com.festora.orderservice.dto.event.OrderCancelledProducerEvent;
import com.festora.orderservice.enums.OrderStatus;
import com.festora.orderservice.gst.GstCalculator;
import com.festora.orderservice.model.Order;
import com.festora.orderservice.model.OrderItem;
import com.festora.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final GstCalculator gstCalculator;
    private final com.festora.orderservice.repository.OrderMenuRedisRepository menuRedisRepo;
    private final MenuClient menuClient;

    @Caching(evict = {
            @CacheEvict(value = "todaysOrders", key = "#req.restaurantId"),
            @CacheEvict(value = "activeOrders", key = "#req.restaurantId")
    })
    public Order createOrder(CreateOrderRequest req) throws Exception {
        if (ObjectUtils.isEmpty(req)) {
            throw new Exception("Order request cant be empty");
        }

        Order order = buildOrder(req);
        // Let MongoDB generate the orderId
        order = orderRepository.save(order);
        
        try {
            inventoryClient.tempReserve(order);
            order.setStatus(OrderStatus.PENDING);
        } catch (Exception e) {
            order.setStatus(OrderStatus.REJECTED);
            order.setUpdatedAt(now());
            orderRepository.save(order);
            throw new IllegalStateException("OUT_OF_STOCK");
        }
        order.setUpdatedAt(now());
        return orderRepository.save(order);
    }

    /* ===============================
        ADD MORE ITEMS (SAFE)
       =============================== */
    @Deprecated
    public Order addItems(String orderId, List<OrderItem> newItems) {

        Order order = get(orderId);

        if (!canAddItems(order)) {
            throw new IllegalStateException("Order is already finalized or being prepared");
        }

        // 🔒 Inventory failure here must NOT cancel order
        try {
            inventoryClient.tempReserve(order, newItems);
        } catch (Exception e) {
            throw new IllegalStateException("ITEM_OUT_OF_STOCK");
        }

        order.getItems().addAll(newItems);
        recalcTotals(order);
        order.setUpdatedAt(now());

        return orderRepository.save(order);
    }

    public void markServed(String orderId) {
        transition(orderId, OrderStatus.PREPARING, OrderStatus.PAYMENT_PENDING);
    }

    /* ===============================
        4️⃣ BILL REQUEST (POSTPAID)
       =============================== */
    public void requestBill(String orderId) {
        transition(orderId, OrderStatus.PAYMENT_PENDING, OrderStatus.PAYMENT_REQUESTED);
    }

    /* ===============================
        5️⃣ PAYMENT SUCCESS (ONE ENTRY)
       =============================== */
    public void onPaymentSuccess(String orderId) {

        Order order = get(orderId);

        // idempotency
        if (order.getStatus() == OrderStatus.PAID ||
                order.getStatus() == OrderStatus.CLOSED) {
            return;
        }

        if (order.getStatus() != OrderStatus.PAYMENT_PENDING &&
                order.getStatus() != OrderStatus.PAYMENT_REQUESTED) {
            throw new IllegalStateException("Invalid payment state");
        }

        inventoryClient.confirm(orderId);

        order.setStatus(OrderStatus.PAID);
        order.setUpdatedAt(now());
        orderRepository.save(order);
    }

    /* ===============================
        6️⃣ CLOSE ORDER
       =============================== */
    public void closeOrder(String orderId) {
        transition(orderId, OrderStatus.PAID, OrderStatus.CLOSED);
    }

    /* ===============================
        7️⃣ CANCEL ORDER (EXPIRY / ADMIN)
       =============================== */
    @Caching(evict = {
            @CacheEvict(value = "singleOrder", key = "#orderId"),
            @CacheEvict(value = "todaysOrders", allEntries = true),
            @CacheEvict(value = "activeOrders", allEntries = true)
    })
    public void cancelOrder(String orderId, String reason) {

        Order order = get(orderId);

        // Never cancel paid orders
        if (order.getStatus() == OrderStatus.PAID ||
                order.getStatus() == OrderStatus.CLOSED) {
            return;
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setReason(reason);
        order.setUpdatedAt(now());
        orderRepository.save(order);

        OrderCancelledProducerEvent cancelEvent = buildOrderCancelledEvent(order);
//        if (cancelEvent != null)
//            orderEventProduce.publishOrderCancelledEvent(cancelEvent);
    }

    private OrderCancelledProducerEvent buildOrderCancelledEvent(Order order) {
        if (ObjectUtils.isEmpty(order)) {
            return null;
        }
        return OrderCancelledProducerEvent.builder()
                .orderId(order.getOrderId())
                .restaurantId(order.getRestaurantId())
                .items(order.getItems())
                .build();
    }
    /* ===============================
       STATE TRANSITION GUARD
       =============================== */
    @Caching(evict = {
            @CacheEvict(value = "singleOrder", key = "#orderId"),
            @CacheEvict(value = "todaysOrders", allEntries = true),
            @CacheEvict(value = "activeOrders", allEntries = true)
    })
    public void transition(String orderId,
                           OrderStatus from,
                           OrderStatus to) {

        Order order = get(orderId);

        if (order.getStatus() != from) {
            throw new IllegalStateException(
                    "Invalid transition: " + from + " → " + order.getStatus()
            );
        }

        order.setStatus(to);
        order.setUpdatedAt(now());
        orderRepository.save(order);
    }

    /* ===============================
       HELPERS
       =============================== */

    private boolean canAddItems(Order order) {
        return order.getStatus() == OrderStatus.CREATED || order.getStatus() == OrderStatus.PENDING;
    }

    private Order get(String id) {
        Order order = orderRepository.findByOrderId(id);
        if (order == null) {
            throw new RuntimeException("Order not found: " + id);
        }
        return order;
    }

    private long now() {
        return System.currentTimeMillis();
    }

    private Order buildOrder(CreateOrderRequest req) {

        List<OrderItem> resolvedItems = req.getItems().stream()
                .map(i -> populateWithMenuDetails(req.getRestaurantId(), i))
                .collect(Collectors.toList());

        double base = resolvedItems.stream()
                .mapToDouble(OrderItem::getTotalPrice)
                .sum();

        GstResult gst = gstCalculator.calculate(req.getRestaurantId(), base);

        return Order.builder()
                .orderId(req.getOrderId())
                .restaurantId(req.getRestaurantId())
                .userId(req.getUserId())
                .deviceId(req.getDeviceId())
                .tableNumber(req.getTableNumber())
                .items(resolvedItems)
                .baseAmount(base)
                .cgstAmount(gst.getCgst())
                .sgstAmount(gst.getSgst())
                .gstAmount(gst.getTotalTax())
                .totalAmount(base + gst.getTotalTax())
                .status(OrderStatus.CREATED)
                .createdAt(now())
                .updatedAt(now())
                .build();
    }

    private OrderItem populateWithMenuDetails(Long restaurantId, OrderItem item) {
        Optional<MenuItemRedis> itemOpt = menuRedisRepo.getMenuItem(item.getMenuItemId());

        if (itemOpt.isPresent()) {
            MenuItemRedis detail = itemOpt.get();
            double basePrice = detail.getBasePrice();
            String variantName = null;

            if (item.getVariantId() != null) {
                MenuItemRedis.VariantRedis variant = detail.getVariants().stream()
                        .filter(v -> v.getId().equals(item.getVariantId()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Invalid variant"));
                basePrice = variant.getPrice();
                variantName = variant.getLabel();
            }

            List<String> addonNames = List.of();
            double addonsPrice = 0;
            if (item.getAddonIds() != null) {
                List<MenuItemRedis.AddonRedis> matchedAddons = detail.getAddons().stream()
                        .filter(a -> item.getAddonIds().contains(a.getId()))
                        .toList();
                addonsPrice = matchedAddons.stream().mapToDouble(MenuItemRedis.AddonRedis::getPrice).sum();
                addonNames = matchedAddons.stream().map(MenuItemRedis.AddonRedis::getName).toList();
            }

            item.setName(detail.getName());
            item.setVariantName(variantName);
            item.setAddonNames(addonNames);
            item.setUnitPrice(basePrice + addonsPrice);
            item.setTotalPrice(item.getUnitPrice() * item.getQuantity());
            return item;
        }

        // Fallback to HTTP
        MenuItemPriceResponse price =
                menuClient.getFinalPrice(
                        item.getMenuItemId(),
                        item.getVariantId(),
                        item.getAddonIds(),
                        restaurantId
                );

        if (price == null || price.getFinalPrice() <= 0) {
            throw new IllegalStateException("Invalid menu price for item : " + item.getMenuItemId());
        }

        item.setName(price.getName());
        item.setVariantName(price.getVariantName());
        item.setAddonNames(price.getAddonNames());
        item.setUnitPrice(price.getFinalPrice());
        item.setTotalPrice(price.getFinalPrice() * item.getQuantity());

        return item;
    }

    private void recalcTotals(Order order) {

        double base = order.getItems().stream()
                .peek(i -> i.setTotalPrice(i.getUnitPrice() * i.getQuantity()))
                .mapToDouble(OrderItem::getTotalPrice)
                .sum();

        GstResult gst = gstCalculator.calculate(
                order.getRestaurantId(), base
        );

        order.setBaseAmount(base);
        order.setCgstAmount(gst.getCgst());
        order.setSgstAmount(gst.getSgst());
        order.setGstAmount(gst.getTotalTax());
        order.setTotalAmount(base + gst.getTotalTax());
    }


    public Order getOrder(String orderId) {
        if (orderId == null) {
            return null;
        }
        return orderRepository.findByOrderId(orderId);
    }

    public void markInventoryBasedOnStatus(InventoryConsumerEvent request) {
        try {
            String orderId = request.getOrderId();
            String status = request.getStatus();

            Order order = getOrder(orderId);
            if (ObjectUtils.isEmpty(order)) {
                log.warn("Order not found for orderId : {}", orderId);
                return;
            }

            if ("TEMP_RESERVED".equalsIgnoreCase(status)) {
                order.setUpdatedAt(now());
                orderRepository.save(order);
                return;
            }

            log.warn("Inventory reservation failed for orderId={} status={}", orderId, status);

            order.setStatus(OrderStatus.REJECTED);
            order.setUpdatedAt(now());
            orderRepository.save(order);

        } catch (Exception e) {
            log.error("Inventory TEMP reserve handling failed for {}", request, e);
        }
    }

    public List<Order> getAllPendingOrders() {
        return orderRepository.findOrdersByStatus(OrderStatus.PENDING);
    }

    @Caching(evict = {
            @CacheEvict(value = "singleOrder", key = "#orderId"),
            @CacheEvict(value = "todaysOrders", allEntries = true),
            @CacheEvict(value = "activeOrders", allEntries = true)
    })
    public Order markOrderConfirm(String orderId) throws Exception {
        if (StringUtils.isBlank(orderId)) {
            throw new Exception("Invalid order id");
        }

        Order order = orderRepository.findByOrderId(orderId);

        if (order == null) {
            throw new Exception("Order not found");
        }

        // confirm inventory ->
        inventoryClient.confirm(orderId);

        // update status
        order.setStatus(OrderStatus.PREPARING);
        order.setUpdatedAt(now());
        return orderRepository.save(order);
    }

    @Caching(evict = {
            @CacheEvict(value = "singleOrder", key = "#orderId"),
            @CacheEvict(value = "todaysOrders", allEntries = true),
            @CacheEvict(value = "activeOrders", allEntries = true)
    })
    public Order updateOrderItems(String orderId, UpdateOrderItemsRequest request) {

        Order order = get(orderId);

        if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Order not editable");
        }

        if (request == null || request.getItems() == null) {
            throw new IllegalArgumentException("Items cannot be empty");
        }

        List<OrderItem> updatedItems = resolveItemsAllowNew(order, request);

        // 🔐 Reserve inventory ONLY for newly added items
        List<OrderItem> newlyAdded = findNewItems(order.getItems(), updatedItems);
        if (!newlyAdded.isEmpty()) {
            inventoryClient.tempReserve(order, newlyAdded);
        }

        order.setItems(updatedItems);
        recalcTotals(order);
        order.setUpdatedAt(now());

        return orderRepository.save(order);
    }

    private List<OrderItem> resolveItemsAllowNew(
            Order order,
            UpdateOrderItemsRequest request
    ) {
        Map<String, OrderItem> existing = order.getItems().stream()
                .collect(Collectors.toMap(this::itemKey, i -> i, (i1, i2) -> {
                    i1.setQuantity(i1.getQuantity() + i2.getQuantity());
                    i1.setTotalPrice(i1.getTotalPrice() + i2.getTotalPrice());
                    return i1;
                }));

        List<OrderItem> result = new ArrayList<>();

        for (ItemUpdate update : request.getItems()) {

            if (update.getQuantity() < 0) {
                throw new IllegalArgumentException("Quantity cannot be negative");
            }

            if (update.getQuantity() == 0) {
                continue; // remove item
            }

            String key = itemKey(
                    update.getMenuItemId(),
                    update.getVariantId(),
                    update.getAddonIds()
            );

            OrderItem item = existing.get(key);

            if (item == null) {
                item = buildNewOrderItem(order, update);
            } else {
                item.setQuantity(update.getQuantity());
            }

            item.setTotalPrice(item.getUnitPrice() * item.getQuantity());
            result.add(item);
        }

        if (result.isEmpty()) {
            throw new IllegalStateException("Order must contain at least one item");
        }

        return result;
    }

    private OrderItem buildNewOrderItem(Order order, ItemUpdate update) {
        Optional<MenuItemRedis> itemOpt = menuRedisRepo.getMenuItem(update.getMenuItemId());

        if (itemOpt.isPresent()) {
            MenuItemRedis detail = itemOpt.get();
            double basePrice = detail.getBasePrice();
            String variantName = null;

            if (update.getVariantId() != null) {
                MenuItemRedis.VariantRedis variant = detail.getVariants().stream()
                        .filter(v -> v.getId().equals(update.getVariantId()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Invalid variant"));
                basePrice = variant.getPrice();
                variantName = variant.getLabel();
            }

            List<String> addonNames = List.of();
            double addonsPrice = 0;
            if (update.getAddonIds() != null) {
                List<MenuItemRedis.AddonRedis> matchedAddons = detail.getAddons().stream()
                        .filter(a -> update.getAddonIds().contains(a.getId()))
                        .toList();
                addonsPrice = matchedAddons.stream().mapToDouble(MenuItemRedis.AddonRedis::getPrice).sum();
                addonNames = matchedAddons.stream().map(MenuItemRedis.AddonRedis::getName).toList();
            }

            return OrderItem.builder()
                    .menuItemId(update.getMenuItemId())
                    .name(detail.getName())
                    .variantId(update.getVariantId())
                    .variantName(variantName)
                    .addonIds(update.getAddonIds())
                    .addonNames(addonNames)
                    .unitPrice(basePrice + addonsPrice)
                    .quantity(update.getQuantity())
                    .totalPrice((basePrice + addonsPrice) * update.getQuantity())
                    .build();
        }

        // Fallback
        MenuItemPriceResponse detail =
                menuClient.getFinalPrice(
                        update.getMenuItemId(),
                        update.getVariantId(),
                        update.getAddonIds(),
                        order.getRestaurantId()
                );

        if (detail == null || detail.getFinalPrice() <= 0) {
            throw new IllegalStateException("Invalid menu price");
        }

        return OrderItem.builder()
                .menuItemId(update.getMenuItemId())
                .name(detail.getName())
                .variantId(update.getVariantId())
                .variantName(detail.getVariantName())
                .addonIds(update.getAddonIds())
                .addonNames(detail.getAddonNames())
                .unitPrice(detail.getFinalPrice())
                .quantity(update.getQuantity())
                .totalPrice(detail.getFinalPrice() * update.getQuantity())
                .build();
    }

    private List<OrderItem> findNewItems(
            List<OrderItem> oldItems,
            List<OrderItem> newItems
    ) {
        Set<String> oldKeys = oldItems.stream()
                .map(this::itemKey)
                .collect(Collectors.toSet());

        return newItems.stream()
                .filter(i -> !oldKeys.contains(itemKey(i)))
                .collect(Collectors.toList());
    }

    @Caching(evict = {
            @CacheEvict(value = "singleOrder", key = "#orderId"),
            @CacheEvict(value = "todaysOrders", allEntries = true),
            @CacheEvict(value = "activeOrders", allEntries = true)
    })
    public Order finalizeOrder(String orderId) {

        Order order = get(orderId);

        if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Order already finalized");
        }

        inventoryClient.confirm(orderId);

        order.setStatus(OrderStatus.PREPARING);
        order.setUpdatedAt(now());

        return orderRepository.save(order);
    }

    @Cacheable(value = "activeOrders", key = "#restaurantId")
    public List<Order> getActiveOwnerOrders(Long restaurantId) {
        log.info("Cache MISS: fetching active orders from DB for restaurant {}", restaurantId);
        return orderRepository.findByRestaurantIdAndStatusIn(restaurantId,
                List.of(
                        OrderStatus.PENDING,
                        OrderStatus.CREATED,
                        OrderStatus.PREPARING,
                        OrderStatus.PAYMENT_PENDING
                )
        );
    }

    private String itemKey(OrderItem item) {
        return itemKey(
                item.getMenuItemId(),
                item.getVariantId(),
                item.getAddonIds()
        );
    }

    private String itemKey(
            String menuItemId,
            String variantId,
            List<String> addonIds
    ) {
        List<String> addons = addonIds == null ? List.of() : addonIds;
        Collections.sort(addons);
        return menuItemId + "|" +
                (variantId == null ? "" : variantId) + "|" +
                String.join(",", addons);
    }

    public List<Order> getAllOrdersForTableByRestaurantId(Long restaurantId, Integer tableNumber, String userId, String deviceId) {
        if (restaurantId == null || tableNumber == null) {
            throw new IllegalArgumentException("Invalid restaurant or table number");
        }
        
        if (deviceId != null && !deviceId.isBlank()) {
            return orderRepository.findOrdersByRestaurantIdAndTableNumberAndUserIdOrDeviceId(restaurantId, tableNumber, userId, deviceId);
        } else {
            return orderRepository.findOrdersByRestaurantIdAndTableNumberAndUserId(restaurantId, tableNumber, userId);
        }
    }

    @Cacheable(value = "todaysOrders", key = "#restaurantId")
    public List<Order> fetchTodaysAllOrders(Long restaurantId) throws Exception {
        if (restaurantId == null) {
            throw new Exception("restaurantId is null");
        }

        log.info("Cache MISS: fetching today's orders from DB for restaurant {}", restaurantId);

        ZoneId kolkataZone = ZoneId.of("Asia/Kolkata");
        LocalDate today = LocalDate.now(kolkataZone);
        long startOfDay = today.atStartOfDay(kolkataZone).toInstant().toEpochMilli();
        long endOfDay = today.plusDays(1).atStartOfDay(kolkataZone).toInstant().toEpochMilli() - 1;

        return orderRepository.findByRestaurantIdAndCreatedAtBetween(restaurantId, startOfDay, endOfDay);
    }
}