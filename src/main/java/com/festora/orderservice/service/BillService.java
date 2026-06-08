package com.festora.orderservice.service;

import com.festora.orderservice.dto.*;
import com.festora.orderservice.enums.BillingStatus;
import com.festora.orderservice.enums.OrderSource;
import com.festora.orderservice.enums.OrderStatus;
import com.festora.orderservice.enums.PaymentMode;
import com.festora.orderservice.enums.SeatingType;
import com.festora.orderservice.gst.GstCalculator;
import com.festora.orderservice.model.Order;
import com.festora.orderservice.model.OrderItem;
import com.festora.orderservice.model.UserBill;
import com.festora.orderservice.repository.OrderRepository;
import com.festora.orderservice.repository.UserBillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillService {

    private final OrderRepository orderRepository;
    private final UserBillRepository userBillRepository;
    private final GstCalculator gstCalculator;
    private final SimpMessagingTemplate messagingTemplate;

    private void broadcastOrderUpdate(Order order) {
        try {
            messagingTemplate.convertAndSend("/topic/orders/" + order.getOrderId(), order);
            messagingTemplate.convertAndSend("/topic/restaurant/" + order.getRestaurantId() + "/orders", order);
        } catch (Exception e) {
            log.error("Failed to broadcast order update in BillService: {}", e.getMessage());
        }
    }

    public List<ActiveTableBillingSummary> getActiveTableBillingSummary(Long restaurantId) {
        // Only fetch PAYMENT_PENDING orders for unbilled summary
        List<Order> unbilledOrders = orderRepository.findByRestaurantIdAndStatus(restaurantId, OrderStatus.PAYMENT_PENDING);
        List<UserBill> activeBills = userBillRepository.findByRestaurantIdAndStatus(restaurantId, BillingStatus.UNPAID);

        // Filter out hotel room service orders and bills from this summary
        unbilledOrders = unbilledOrders.stream()
                .filter(o -> o.getSeatingType() != SeatingType.HOTEL_ROOM)
                .collect(Collectors.toList());
        activeBills = activeBills.stream()
                .filter(b -> b.getSeatingType() != SeatingType.HOTEL_ROOM)
                .collect(Collectors.toList());

        Map<String, ActiveTableBillingSummary> summaryMap = new HashMap<>();

        // Group unbilled orders using composite key (seatingType + "_" + tableNumber) to avoid collisions
        for (Order order : unbilledOrders) {
            SeatingType st = order.getSeatingType() != null ? order.getSeatingType() : SeatingType.TABLE;
            String key = st + "_" + order.getTableNumber();
            summaryMap.computeIfAbsent(key, k -> 
                ActiveTableBillingSummary.builder()
                        .tableNumber(order.getTableNumber())
                        .seatingType(st)
                        .unbilledOrdersCount(0)
                        .activeBillsCount(0)
                        .totalUnpaidAmount(0)
                        .build()
            );
            
            ActiveTableBillingSummary summary = summaryMap.get(key);
            summary.setUnbilledOrdersCount(summary.getUnbilledOrdersCount() + 1);
            summary.setTotalUnpaidAmount(summary.getTotalUnpaidAmount() + order.getTotalAmount());
        }

        // Group active bills using composite key to avoid collisions
        for (UserBill bill : activeBills) {
            SeatingType st = bill.getSeatingType() != null ? bill.getSeatingType() : SeatingType.TABLE;
            String key = st + "_" + bill.getTableNumber();
            summaryMap.computeIfAbsent(key, k -> 
                ActiveTableBillingSummary.builder()
                        .tableNumber(bill.getTableNumber())
                        .seatingType(st)
                        .unbilledOrdersCount(0)
                        .activeBillsCount(0)
                        .totalUnpaidAmount(0)
                        .build()
            );
            
            ActiveTableBillingSummary summary = summaryMap.get(key);
            summary.setActiveBillsCount(summary.getActiveBillsCount() + 1);
            summary.setTotalUnpaidAmount(summary.getTotalUnpaidAmount() + bill.getTotalAmount());
        }

        return new ArrayList<>(summaryMap.values());
    }

    public TableBillingResponse getTableBilling(Long restaurantId, int tableNumber) {
        return getTableBilling(restaurantId, tableNumber, SeatingType.TABLE);
    }

    public TableBillingResponse getTableBilling(Long restaurantId, int tableNumber, SeatingType seatingType) {
        SeatingType st = seatingType != null ? seatingType : SeatingType.TABLE;
        // Only fetch PAYMENT_PENDING orders for unbilled grouping
        List<Order> unbilledOrders = orderRepository.findByRestaurantIdAndTableNumberAndSeatingTypeAndStatus(restaurantId, tableNumber, st, OrderStatus.PAYMENT_PENDING);
        List<UserBill> activeBills = userBillRepository.findByRestaurantIdAndTableNumberAndSeatingTypeAndStatusOrderByCreatedAtDesc(restaurantId, tableNumber, st, BillingStatus.UNPAID);

        Map<String, List<Order>> ordersByUser = unbilledOrders.stream()
                .collect(Collectors.groupingBy(Order::getUserId));

        List<TableUserOrderGroup> groups = new ArrayList<>();
        int guestCount = 1;
        for (Map.Entry<String, List<Order>> entry : ordersByUser.entrySet()) {
            List<Order> userOrders = entry.getValue();
            String label = userOrders.stream()
                    .map(Order::getUserName)
                    .filter(name -> name != null && !name.isBlank())
                    .findFirst()
                    .orElse("Guest " + guestCount++);

            double total = userOrders.stream().mapToDouble(Order::getTotalAmount).sum();
            groups.add(TableUserOrderGroup.builder()
                    .userId(entry.getKey())
                    .label(label)
                    .orders(userOrders)
                    .totalAmount(total)
                    .build());
        }

        return TableBillingResponse.builder()
                .tableNumber(tableNumber)
                .unbilledOrders(groups)
                .activeBills(activeBills)
                .build();
    }

    public UserBill generateBill(Long restaurantId, int tableNumber, String userId) {
        return generateBill(restaurantId, tableNumber, SeatingType.TABLE, userId);
    }

    public UserBill generateBill(Long restaurantId, int tableNumber, SeatingType seatingType, String userId) {
        SeatingType st = seatingType != null ? seatingType : SeatingType.TABLE;
        // Only generate bill from PAYMENT_PENDING orders
        List<Order> unbilledOrders = orderRepository.findByRestaurantIdAndTableNumberAndSeatingTypeAndUserIdAndStatus(restaurantId, tableNumber, st, userId, OrderStatus.PAYMENT_PENDING);
        
        if (unbilledOrders.isEmpty()) {
            throw new IllegalStateException("No unbilled orders found for user at this table");
        }

        List<String> orderIds = new ArrayList<>();
        List<OrderItem> allItems = new ArrayList<>();
        double baseAmount = 0;

        for (Order order : unbilledOrders) {
            orderIds.add(order.getOrderId());
            allItems.addAll(order.getItems());
            baseAmount += order.getBaseAmount();
        }

        GstResult gst = gstCalculator.calculate(restaurantId, baseAmount);

        String userName = unbilledOrders.stream()
                .map(Order::getUserName)
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElse("Guest");

        // Derive bill context from first order (all orders in group share same context)
        Order representative = unbilledOrders.get(0);

        UserBill bill = UserBill.builder()
                .billId(generateBillId())
                .restaurantId(restaurantId)
                .tableNumber(tableNumber)
                .seatingType(st)
                .orderSource(representative.getOrderSource() != null ? representative.getOrderSource() : OrderSource.DINE_IN)
                .hotelConfigId(representative.getHotelConfigId())
                .hotelName(representative.getHotelName())
                .mobileNumber(representative.getMobileNumber())
                .roomNumber(representative.getRoomNumber())
                .paymentMode(representative.getPaymentMode() != null ? representative.getPaymentMode() : PaymentMode.CASH_ON_DELIVERY)
                .userId(userId)
                .userName(userName)
                .orderIds(orderIds)
                .items(allItems)
                .baseAmount(baseAmount)
                .cgstAmount(gst.getCgst())
                .sgstAmount(gst.getSgst())
                .gstAmount(gst.getTotalTax())
                .totalAmount(baseAmount + gst.getTotalTax())
                .status(BillingStatus.UNPAID)
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();

        UserBill savedBill = userBillRepository.save(bill);

        for (Order order : unbilledOrders) {
            order.setStatus(OrderStatus.PAYMENT_REQUESTED);
            order.setBillId(savedBill.getBillId());
            order.setUpdatedAt(System.currentTimeMillis());
            orderRepository.save(order);
            broadcastOrderUpdate(order);
        }

        return savedBill;
    }

    public UserBill markBillAsPaid(String billId) {
        UserBill bill = userBillRepository.findByBillId(billId);
        if (bill == null) {
            throw new IllegalArgumentException("Bill not found: " + billId);
        }
        
        if (BillingStatus.PAID.equals(bill.getStatus())) {
            return bill;
        }

        bill.setStatus(BillingStatus.PAID);
        bill.setUpdatedAt(System.currentTimeMillis());
        userBillRepository.save(bill);

        for (String orderId : bill.getOrderIds()) {
            Order order = orderRepository.findByOrderId(orderId);
            if (order != null) {
                order.setStatus(OrderStatus.PAID);
                order.setUpdatedAt(System.currentTimeMillis());
                orderRepository.save(order);
                broadcastOrderUpdate(order);
            }
        }

        return bill;
    }

    public List<UserBill> getPaidBills(Long restaurantId) {
        long startOfToday = java.time.LocalDate.now()
                .atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
        return userBillRepository.findByRestaurantIdAndStatusAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                restaurantId, BillingStatus.PAID, startOfToday);
    }

    public void closeTable(Long restaurantId, int tableNumber) {
        closeTable(restaurantId, tableNumber, SeatingType.TABLE);
    }

    public void closeTable(Long restaurantId, int tableNumber, SeatingType seatingType) {
        SeatingType st = seatingType != null ? seatingType : SeatingType.TABLE;
        List<Order> orders = orderRepository.findByRestaurantIdAndTableNumberAndSeatingTypeAndStatus(restaurantId, tableNumber, st, OrderStatus.PAID);
        for (Order order : orders) {
            order.setStatus(OrderStatus.CLOSED);
            order.setUpdatedAt(System.currentTimeMillis());
            orderRepository.save(order);
            broadcastOrderUpdate(order);
        }
    }

    // ── Hotel room service billing methods ──────────────────────────────────────────

    public List<ActiveHotelBillingSummary> getActiveHotelBillingSummary(Long restaurantId, String hotelConfigId) {
        List<Order> unbilledOrders = orderRepository.findByRestaurantIdAndHotelConfigIdAndStatus(restaurantId, hotelConfigId, OrderStatus.PAYMENT_PENDING);
        List<UserBill> activeBills = userBillRepository.findByRestaurantIdAndHotelConfigIdAndStatus(restaurantId, hotelConfigId, BillingStatus.UNPAID);

        Map<String, ActiveHotelBillingSummary> summaryMap = new HashMap<>();

        // Group unbilled orders by roomNumber
        for (Order order : unbilledOrders) {
            String room = order.getRoomNumber() != null ? order.getRoomNumber() : "Unknown";
            summaryMap.computeIfAbsent(room, r -> 
                ActiveHotelBillingSummary.builder()
                        .roomNumber(r)
                        .unbilledOrdersCount(0)
                        .activeBillsCount(0)
                        .totalUnpaidAmount(0)
                        .build()
            );
            ActiveHotelBillingSummary summary = summaryMap.get(room);
            summary.setUnbilledOrdersCount(summary.getUnbilledOrdersCount() + 1);
            summary.setTotalUnpaidAmount(summary.getTotalUnpaidAmount() + order.getTotalAmount());
        }

        // Group active bills by roomNumber
        for (UserBill bill : activeBills) {
            String room = bill.getRoomNumber() != null ? bill.getRoomNumber() : "Unknown";
            summaryMap.computeIfAbsent(room, r -> 
                ActiveHotelBillingSummary.builder()
                        .roomNumber(r)
                        .unbilledOrdersCount(0)
                        .activeBillsCount(0)
                        .totalUnpaidAmount(0)
                        .build()
            );
            ActiveHotelBillingSummary summary = summaryMap.get(room);
            summary.setActiveBillsCount(summary.getActiveBillsCount() + 1);
            summary.setTotalUnpaidAmount(summary.getTotalUnpaidAmount() + bill.getTotalAmount());
        }

        return new ArrayList<>(summaryMap.values());
    }

    public HotelRoomBillingResponse getHotelRoomBilling(Long restaurantId, String hotelConfigId, String roomNumber) {
        List<Order> unbilledOrders = orderRepository.findByRestaurantIdAndHotelConfigIdAndRoomNumberAndStatus(restaurantId, hotelConfigId, roomNumber, OrderStatus.PAYMENT_PENDING);
        List<UserBill> activeBills = userBillRepository.findByRestaurantIdAndHotelConfigIdAndRoomNumberAndStatusOrderByCreatedAtDesc(restaurantId, hotelConfigId, roomNumber, BillingStatus.UNPAID);

        Map<String, List<Order>> ordersByUser = unbilledOrders.stream()
                .collect(Collectors.groupingBy(Order::getUserId));

        List<TableUserOrderGroup> groups = new ArrayList<>();
        int guestCount = 1;
        for (Map.Entry<String, List<Order>> entry : ordersByUser.entrySet()) {
            List<Order> userOrders = entry.getValue();
            String label = userOrders.stream()
                    .map(Order::getUserName)
                    .filter(name -> name != null && !name.isBlank())
                    .findFirst()
                    .orElse("Guest " + guestCount++);

            double total = userOrders.stream().mapToDouble(Order::getTotalAmount).sum();
            groups.add(TableUserOrderGroup.builder()
                    .userId(entry.getKey())
                    .label(label)
                    .orders(userOrders)
                    .totalAmount(total)
                    .build());
        }

        return HotelRoomBillingResponse.builder()
                .roomNumber(roomNumber)
                .unbilledOrders(groups)
                .activeBills(activeBills)
                .build();
    }

    public UserBill generateHotelBill(Long restaurantId, String hotelConfigId, String roomNumber, String userId) {
        List<Order> unbilledOrders = orderRepository.findByRestaurantIdAndHotelConfigIdAndRoomNumberAndUserIdAndStatus(restaurantId, hotelConfigId, roomNumber, userId, OrderStatus.PAYMENT_PENDING);
        
        if (unbilledOrders.isEmpty()) {
            throw new IllegalStateException("No unbilled orders found for user in this room");
        }

        List<String> orderIds = new ArrayList<>();
        List<OrderItem> allItems = new ArrayList<>();
        double baseAmount = 0;

        for (Order order : unbilledOrders) {
            orderIds.add(order.getOrderId());
            allItems.addAll(order.getItems());
            baseAmount += order.getBaseAmount();
        }

        GstResult gst = gstCalculator.calculate(restaurantId, baseAmount);

        String userName = unbilledOrders.stream()
                .map(Order::getUserName)
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElse("Guest");

        Order representative = unbilledOrders.get(0);

        UserBill bill = UserBill.builder()
                .billId(generateBillId())
                .restaurantId(restaurantId)
                .tableNumber(0)
                .seatingType(SeatingType.HOTEL_ROOM)
                .orderSource(OrderSource.HOTEL_ROOM_SERVICE)
                .hotelConfigId(hotelConfigId)
                .hotelName(representative.getHotelName())
                .mobileNumber(representative.getMobileNumber())
                .roomNumber(roomNumber)
                .paymentMode(representative.getPaymentMode() != null ? representative.getPaymentMode() : PaymentMode.CASH_ON_DELIVERY)
                .userId(userId)
                .userName(userName)
                .orderIds(orderIds)
                .items(allItems)
                .baseAmount(baseAmount)
                .cgstAmount(gst.getCgst())
                .sgstAmount(gst.getSgst())
                .gstAmount(gst.getTotalTax())
                .totalAmount(baseAmount + gst.getTotalTax())
                .status(BillingStatus.UNPAID)
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();

        UserBill savedBill = userBillRepository.save(bill);

        for (Order order : unbilledOrders) {
            order.setStatus(OrderStatus.PAYMENT_REQUESTED);
            order.setBillId(savedBill.getBillId());
            order.setUpdatedAt(System.currentTimeMillis());
            orderRepository.save(order);
            broadcastOrderUpdate(order);
        }

        return savedBill;
    }

    public void closeHotelRoom(Long restaurantId, String hotelConfigId, String roomNumber) {
        List<Order> orders = orderRepository.findByRestaurantIdAndHotelConfigIdAndRoomNumberAndStatusIn(
                restaurantId, hotelConfigId, roomNumber, List.of(OrderStatus.PAID));
        for (Order order : orders) {
            order.setStatus(OrderStatus.CLOSED);
            order.setUpdatedAt(System.currentTimeMillis());
            orderRepository.save(order);
            broadcastOrderUpdate(order);
        }
    }

    private String generateBillId() {
        String alphanumeric = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder("B-");
        Random random = new Random();
        for (int i = 0; i < 6; i++) {
            sb.append(alphanumeric.charAt(random.nextInt(alphanumeric.length())));
        }
        return sb.toString();
    }
    public UserBill getMyLatestBill(String userId, Long restaurantId) {
        return userBillRepository.findTopByUserIdAndRestaurantIdOrderByCreatedAtDesc(userId, restaurantId);
    }
}
