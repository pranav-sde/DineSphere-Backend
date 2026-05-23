package com.festora.hotelservice.service;

import com.festora.hotelservice.dto.CreateHotelOrderRequest;
import com.festora.hotelservice.model.HotelConfig;
import com.festora.orderservice.client.InventoryClient;
import com.festora.orderservice.client.MenuClient;
import com.festora.orderservice.dto.GstResult;
import com.festora.orderservice.dto.MenuItemPriceResponse;
import com.festora.orderservice.enums.OrderSource;
import com.festora.orderservice.enums.OrderStatus;
import com.festora.orderservice.enums.PaymentMode;
import com.festora.orderservice.enums.SeatingType;
import com.festora.orderservice.gst.GstCalculator;
import com.festora.orderservice.model.Order;
import com.festora.orderservice.model.OrderItem;
import com.festora.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Handles the hotel room-service order flow.
 *
 * Design: Delegates inventory + menu resolution to the same clients used by OrderService,
 * keeping GST, pricing, and inventory reservation consistent across all order types.
 * This avoids duplication while keeping hotel-specific logic isolated in its own service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HotelOrderService {

    private final OrderRepository orderRepository;
    private final HotelConfigService hotelConfigService;
    private final HotelRoomConfigService roomConfigService;
    private final InventoryClient inventoryClient;
    private final MenuClient menuClient;
    private final GstCalculator gstCalculator;
    private final SimpMessagingTemplate messagingTemplate;

    public Order createHotelOrder(CreateHotelOrderRequest req) throws Exception {

        // 1. Validate inputs
        validateRequest(req);

        // 2. Load hotel context
        HotelConfig hotel = hotelConfigService.getById(req.getHotelConfigId());
        if (!Boolean.TRUE.equals(hotel.getActive())) {
            throw new IllegalArgumentException("Hotel is no longer active");
        }

        // 3. Validate room number (if validation enabled for this hotel)
        boolean roomValid = roomConfigService.validateRoom(req.getHotelConfigId(), req.getRoomNumber());
        if (!roomValid) {
            throw new IllegalArgumentException(
                    "Room '" + req.getRoomNumber() + "' not found in " + hotel.getHotelName()
                            + ". Please check your room number.");
        }

        // 4. Resolve menu details and build order
        Order order = buildHotelOrder(req, hotel);

        // 5. Reserve inventory (same flow as dine-in)
        try {
            inventoryClient.tempReserve(order);
            order.setStatus(OrderStatus.PENDING);
        } catch (Exception e) {
            order.setStatus(OrderStatus.REJECTED);
            order.setUpdatedAt(now());
            saveAndBroadcast(order);
            throw new IllegalStateException(e.getMessage());
        }

        order.setUpdatedAt(now());
        return saveAndBroadcast(order);
    }

    public List<Order> getOrdersByMobileAndHotel(String mobileNumber, String hotelConfigId) {
        if (mobileNumber == null || mobileNumber.isBlank()) {
            throw new IllegalArgumentException("Mobile number is required");
        }
        if (hotelConfigId == null || hotelConfigId.isBlank()) {
            throw new IllegalArgumentException("Hotel ID is required");
        }
        return orderRepository.findByMobileNumberAndHotelConfigIdOrderByCreatedAtDesc(
                mobileNumber, hotelConfigId);
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private void validateRequest(CreateHotelOrderRequest req) {
        if (req.getHotelConfigId() == null || req.getHotelConfigId().isBlank()) {
            throw new IllegalArgumentException("hotelConfigId is required");
        }
        if (req.getMobileNumber() == null || req.getMobileNumber().isBlank()) {
            throw new IllegalArgumentException("Mobile number is required");
        }
        if (req.getRoomNumber() == null || req.getRoomNumber().isBlank()) {
            throw new IllegalArgumentException("Room number is required");
        }
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }
    }

    private Order buildHotelOrder(CreateHotelOrderRequest req, HotelConfig hotel) {
        List<OrderItem> resolvedItems = req.getItems().stream()
                .map(item -> populateWithMenuDetails(hotel.getRestaurantId(), item))
                .collect(Collectors.toList());

        double base = resolvedItems.stream()
                .mapToDouble(OrderItem::getTotalPrice)
                .sum();

        GstResult gst = gstCalculator.calculate(hotel.getRestaurantId(), base);

        return Order.builder()
                .orderId(generateOrderId())
                .restaurantId(hotel.getRestaurantId())
                .userId("HOTEL-" + req.getMobileNumber())    // synthetic userId for hotel guests
                .userName(req.getMobileNumber())
                .deviceId(null)
                .tableNumber(0)                              // not applicable for hotel orders
                // Seating & source
                .seatingType(SeatingType.HOTEL_ROOM)
                .orderSource(OrderSource.HOTEL_ROOM_SERVICE)
                // Hotel identity
                .hotelConfigId(hotel.getId())
                .hotelName(hotel.getHotelName())
                .mobileNumber(req.getMobileNumber())
                .roomNumber(req.getRoomNumber())
                // Payment
                .paymentMode(req.getPaymentMode() != null
                        ? PaymentMode.valueOf(req.getPaymentMode())
                        : PaymentMode.ONLINE)
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
        MenuItemPriceResponse price = menuClient.getFinalPrice(
                item.getMenuItemId(),
                item.getVariantId(),
                item.getAddonIds(),
                restaurantId
        );

        if (price == null || price.getFinalPrice() <= 0) {
            throw new IllegalStateException("INVALID_MENU_PRICE for item: " + item.getMenuItemId());
        }

        item.setName(price.getName());
        item.setVariantName(price.getVariantName());
        item.setAddonNames(price.getAddonNames());
        item.setUnitPrice(price.getFinalPrice());
        item.setTotalPrice(price.getFinalPrice() * item.getQuantity());
        return item;
    }

    private Order saveAndBroadcast(Order order) {
        Order saved = orderRepository.save(order);
        try {
            messagingTemplate.convertAndSend("/topic/orders/" + saved.getOrderId(), saved);
            messagingTemplate.convertAndSend("/topic/restaurant/" + saved.getRestaurantId() + "/orders", saved);
        } catch (Exception e) {
            log.error("Failed to broadcast hotel order update: {}", e.getMessage());
        }
        return saved;
    }

    private long now() {
        return System.currentTimeMillis();
    }

    private String generateOrderId() {
        String alphanumeric = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder("HTL_");
        Random random = new Random();
        for (int i = 0; i < 8; i++) {
            sb.append(alphanumeric.charAt(random.nextInt(alphanumeric.length())));
        }
        return sb.toString();
    }
}