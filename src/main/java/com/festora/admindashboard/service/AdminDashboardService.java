package com.festora.admindashboard.service;

import com.festora.admindashboard.dto.*;
import com.festora.barservice.model.BarInventoryItem;
import com.festora.barservice.repository.BarInventoryRepository;
import com.festora.captainservice.model.CaptainAction;
import com.festora.captainservice.model.TableZone;
import com.festora.captainservice.repository.CaptainActionRepository;
import com.festora.captainservice.repository.TableZoneRepository;
import com.festora.kitchenservice.enums.KitchenStation;
import com.festora.kitchenservice.enums.TicketStatus;
import com.festora.kitchenservice.model.KitchenTicket;
import com.festora.kitchenservice.model.TicketItem;
import com.festora.kitchenservice.repository.KitchenTicketRepository;
import com.festora.orderservice.enums.OrderStatus;
import com.festora.orderservice.model.Order;
import com.festora.orderservice.model.TableRequest;
import com.festora.orderservice.repository.OrderRepository;
import com.festora.orderservice.repository.TableRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardService {

    private final OrderRepository orderRepository;
    private final TableRequestRepository tableRequestRepository;
    private final TableZoneRepository zoneRepository;
    private final KitchenTicketRepository kitchenTicketRepository;
    private final BarInventoryRepository barInventoryRepository;
    private final CaptainActionRepository captainActionRepository;

    public List<TableStatusView> getLiveFloorView(Long restaurantId) {
        // Fetch all active orders
        List<Order> activeOrders = orderRepository.findByRestaurantIdAndStatusNotIn(
            restaurantId, List.of(OrderStatus.PAID, OrderStatus.CLOSED, OrderStatus.CANCELLED)
        );

        // Map table number to orders
        Map<Integer, List<Order>> tableOrderMap = activeOrders.stream()
            .collect(Collectors.groupingBy(Order::getTableNumber));

        // Fetch all active table calls / requests
        List<TableRequest> activeRequests = tableRequestRepository.findByRestaurantIdAndStatusOrderByCreatedAtDesc(
            restaurantId, "PENDING"
        );
        Map<Integer, List<TableRequest>> tableRequestMap = activeRequests.stream()
            .collect(Collectors.groupingBy(TableRequest::getTableNumber));

        // Fetch zones to map table to captain
        List<TableZone> zones = zoneRepository.findByRestaurantId(restaurantId);

        List<TableStatusView> floorView = new ArrayList<>();

        for (int tableNo = 1; tableNo <= 100; tableNo++) {
            List<Order> orders = tableOrderMap.getOrDefault(tableNo, new ArrayList<>());
            List<TableRequest> requests = tableRequestMap.getOrDefault(tableNo, new ArrayList<>());

            String status = "EMPTY";
            String color = "white";
            double totalAmount = 0.0;
            long minutesSinceLastUpdate = 0;
            boolean hasAlerts = !requests.isEmpty();

            // Find assigned captain
            final int tNo = tableNo;
            TableZone assignedZone = zones.stream()
                .filter(z -> z.getTableNumbers() != null && z.getTableNumbers().contains(tNo))
                .findFirst()
                .orElse(null);
            String captainName = assignedZone != null ? assignedZone.getAssignedCaptainName() : "";

            if (!orders.isEmpty()) {
                Order primaryOrder = orders.get(0);
                totalAmount = orders.stream().mapToDouble(Order::getTotalAmount).sum();
                minutesSinceLastUpdate = (System.currentTimeMillis() - primaryOrder.getUpdatedAt()) / (60 * 1000);

                OrderStatus orderStatus = primaryOrder.getStatus();
                if (orderStatus == OrderStatus.CREATED || orderStatus == OrderStatus.PENDING) {
                    status = "ORDERING";
                    color = "green";
                } else if (orderStatus == OrderStatus.PREPARING || orderStatus == OrderStatus.IN_KITCHEN) {
                    status = "IN_KITCHEN";
                    color = "blue";
                    if (minutesSinceLastUpdate > 20) {
                        hasAlerts = true;
                    }
                } else if (orderStatus == OrderStatus.READY_TO_SERVE) {
                    status = "READY_TO_SERVE";
                    color = "orange";
                } else if (orderStatus == OrderStatus.PAYMENT_PENDING || orderStatus == OrderStatus.PAYMENT_REQUESTED) {
                    status = "BILL_REQUESTED";
                    color = "yellow";
                } else {
                    status = "OCCUPIED";
                    color = "green";
                }
            }

            if (hasAlerts) {
                status = "ALERT";
                color = "red";
            }

            floorView.add(TableStatusView.builder()
                .tableNumber(tableNo)
                .status(status)
                .color(color)
                .activeOrders(orders.size())
                .totalAmount(totalAmount)
                .minutesSinceLastUpdate(minutesSinceLastUpdate)
                .captainName(captainName)
                .hasAlerts(hasAlerts)
                .build());
        }

        return floorView;
    }

    public Map<String, Integer> getKitchenLoad(Long restaurantId) {
        List<KitchenTicket> activeTickets = kitchenTicketRepository.findByRestaurantIdAndStatusIn(
            restaurantId, List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS)
        );

        Map<String, Integer> load = new HashMap<>();
        for (KitchenStation station : KitchenStation.values()) {
            load.put(station.name(), 0);
        }

        for (KitchenTicket ticket : activeTickets) {
            if (ticket.getStation() != null && ticket.getItems() != null) {
                String key = ticket.getStation().name();
                load.put(key, load.getOrDefault(key, 0) + ticket.getItems().stream().mapToInt(TicketItem::getQuantity).sum());
            }
        }

        return load;
    }

    public DailyDashboardSummary getTodaySummary(Long restaurantId) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfDay = cal.getTimeInMillis();
        long endOfDay = System.currentTimeMillis();

        List<Order> orders = orderRepository.findByRestaurantIdAndCreatedAtBetween(restaurantId, startOfDay, endOfDay);

        double totalRevenue = orders.stream()
            .filter(o -> o.getStatus() == OrderStatus.PAID || o.getStatus() == OrderStatus.CLOSED)
            .mapToDouble(Order::getTotalAmount)
            .sum();

        long totalOrdersCount = orders.size();
        double averageOrderValue = totalOrdersCount > 0 ? totalRevenue / totalOrdersCount : 0.0;

        long activeTablesCount = orders.stream()
            .filter(o -> o.getStatus() != OrderStatus.PAID && o.getStatus() != OrderStatus.CLOSED && o.getStatus() != OrderStatus.CANCELLED)
            .map(Order::getTableNumber)
            .distinct()
            .count();

        List<KitchenTicket> pendingTickets = kitchenTicketRepository.findByRestaurantIdAndStatusIn(
            restaurantId, List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS)
        );

        return DailyDashboardSummary.builder()
            .totalRevenue(totalRevenue)
            .totalOrdersCount(totalOrdersCount)
            .averageOrderValue(averageOrderValue)
            .activeTablesCount((int) activeTablesCount)
            .pendingKitchenTicketsCount(pendingTickets.size())
            .build();
    }

    public List<AdminAlert> getActiveAlerts(Long restaurantId) {
        List<AdminAlert> alerts = new ArrayList<>();

        // 1. Delayed orders (>20 mins)
        List<Order> activeOrders = orderRepository.findByRestaurantIdAndStatusNotIn(
            restaurantId, List.of(OrderStatus.PAID, OrderStatus.CLOSED, OrderStatus.CANCELLED)
        );
        for (Order order : activeOrders) {
            if (order.getStatus() == OrderStatus.PREPARING || order.getStatus() == OrderStatus.IN_KITCHEN) {
                long durationMins = (System.currentTimeMillis() - order.getUpdatedAt()) / (60 * 1000);
                if (durationMins > 20) {
                    alerts.add(AdminAlert.builder()
                        .alertId(UUID.randomUUID().toString())
                        .type("DELAYED_ORDER")
                        .message(String.format("Order #%s at Table %d has been preparing for %d minutes",
                            order.getOrderId().substring(0, Math.min(order.getOrderId().length(), 8)),
                            order.getTableNumber(), durationMins))
                        .timestamp(order.getUpdatedAt())
                        .severity("CRITICAL")
                        .build());
                }
            }
        }

        // 2. Low stock items
        List<BarInventoryItem> lowStockItems = barInventoryRepository.findByRestaurantId(restaurantId).stream()
            .filter(item -> item.getAvailableStock() <= item.getLowStockThreshold())
            .collect(Collectors.toList());
        for (BarInventoryItem item : lowStockItems) {
            alerts.add(AdminAlert.builder()
                .alertId(UUID.randomUUID().toString())
                .type("LOW_STOCK")
                .message(String.format("Low Stock alert: %s is down to %.1f %s",
                    item.getItemName(), item.getAvailableStock(), item.getUnit() != null ? item.getUnit().name() : "units"))
                .timestamp(item.getUpdatedAt())
                .severity("WARNING")
                .build());
        }

        // 3. Captain calls
        List<TableRequest> pendingRequests = tableRequestRepository.findByRestaurantIdAndStatusOrderByCreatedAtDesc(
            restaurantId, "PENDING"
        );
        for (TableRequest req : pendingRequests) {
            alerts.add(AdminAlert.builder()
                .alertId(UUID.randomUUID().toString())
                .type("CAPTAIN_CALL")
                .message(String.format("Table %d requests: %s", req.getTableNumber(), req.getType().replace("_", " ")))
                .timestamp(req.getCreatedAt())
                .severity("INFO")
                .build());
        }

        return alerts;
    }

    public List<CaptainPerformance> getCaptainPerformance(Long restaurantId, long from, long to) {
        List<CaptainAction> actions = captainActionRepository.findByRestaurantIdAndTimestampBetween(restaurantId, from, to);

        Map<String, List<CaptainAction>> captainGroup = actions.stream()
            .filter(a -> a.getCaptainId() != null)
            .collect(Collectors.groupingBy(CaptainAction::getCaptainId));

        List<CaptainPerformance> report = new ArrayList<>();

        for (Map.Entry<String, List<CaptainAction>> entry : captainGroup.entrySet()) {
            String captainId = entry.getKey();
            List<CaptainAction> captainActions = entry.getValue();

            String captainName = captainActions.isEmpty() ? "" : captainActions.get(0).getCaptainName();
            if (captainName == null || captainName.isEmpty()) {
                captainName = captainId;
            }

            long totalActions = captainActions.size();
            long tablesServed = captainActions.stream()
                .filter(a -> "SERVED".equals(a.getActionType()))
                .count();

            double avgServeTimeMins = 0.0;
            List<Long> serveTimes = new ArrayList<>();

            for (CaptainAction action : captainActions) {
                if ("SERVED".equals(action.getActionType()) && action.getTicketId() != null) {
                    try {
                        Optional<KitchenTicket> ticketOpt = kitchenTicketRepository.findByTicketId(action.getTicketId());
                        if (ticketOpt.isPresent()) {
                            KitchenTicket ticket = ticketOpt.get();
                            if (ticket.getReadyAt() > 0 && action.getTimestamp() > ticket.getReadyAt()) {
                                long serveTimeMs = action.getTimestamp() - ticket.getReadyAt();
                                serveTimes.add(serveTimeMs);
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Failed to retrieve ticket for serve time calculation: {}", e.getMessage());
                    }
                }
            }

            if (!serveTimes.isEmpty()) {
                avgServeTimeMins = serveTimes.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0.0) / (60 * 1000.0);
            }

            report.add(CaptainPerformance.builder()
                .captainId(captainId)
                .captainName(captainName)
                .tablesServedCount(tablesServed)
                .averageServeTimeMinutes(Math.round(avgServeTimeMins * 10.0) / 10.0)
                .totalActionsCount(totalActions)
                .build());
        }

        return report;
    }
}
