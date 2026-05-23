package com.festora.orderservice.controller;

import com.festora.orderservice.dto.ActiveTableBillingSummary;
import com.festora.orderservice.dto.ActiveHotelBillingSummary;
import com.festora.orderservice.dto.GenerateBillRequest;
import com.festora.orderservice.dto.GenerateHotelBillRequest;
import com.festora.orderservice.dto.HotelRoomBillingResponse;
import com.festora.orderservice.dto.TableBillingResponse;
import com.festora.orderservice.enums.SeatingType;
import com.festora.orderservice.model.UserBill;
import com.festora.orderservice.service.BillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/order/admin/billing")
@RequiredArgsConstructor
public class AdminBillController {

    private final BillService billService;

    @GetMapping("/tables")
    public ResponseEntity<List<ActiveTableBillingSummary>> getActiveTableBillingSummary(
            @RequestHeader("X-Restaurant-Id") Long restaurantId) {
        return ResponseEntity.ok(billService.getActiveTableBillingSummary(restaurantId));
    }

    @GetMapping("/table/{tableNumber}")
    public ResponseEntity<TableBillingResponse> getTableBilling(
            @RequestHeader("X-Restaurant-Id") Long restaurantId,
            @PathVariable int tableNumber,
            @RequestParam(required = false) SeatingType seatingType) {
        return ResponseEntity.ok(billService.getTableBilling(restaurantId, tableNumber, seatingType));
    }

    @PostMapping("/generate")
    public ResponseEntity<UserBill> generateBill(
            @RequestHeader("X-Restaurant-Id") Long restaurantId,
            @RequestBody GenerateBillRequest request) {
        
        // Ensure restaurantId matches
        if (!restaurantId.equals(request.getRestaurantId())) {
            return ResponseEntity.badRequest().build();
        }

        try {
            return ResponseEntity.ok(billService.generateBill(restaurantId, request.getTableNumber(), request.getSeatingType(), request.getUserId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{billId}/pay")
    public ResponseEntity<UserBill> markBillAsPaid(
            @RequestHeader("X-Restaurant-Id") Long restaurantId, // Used for validation if needed
            @PathVariable String billId) {
        try {
            return ResponseEntity.ok(billService.markBillAsPaid(billId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/paid")
    public ResponseEntity<List<UserBill>> getPaidBills(
            @RequestHeader("X-Restaurant-Id") Long restaurantId) {
        return ResponseEntity.ok(billService.getPaidBills(restaurantId));
    }

    @PostMapping("/table/{tableNumber}/close")
    public ResponseEntity<Void> closeTable(
            @RequestHeader("X-Restaurant-Id") Long restaurantId,
            @PathVariable int tableNumber,
            @RequestParam(required = false) SeatingType seatingType) {
        billService.closeTable(restaurantId, tableNumber, seatingType);
        return ResponseEntity.ok().build();
    }

    // ── Hotel-specific Room Service Billing Endpoints ─────────────────────────────────────

    @GetMapping("/hotel/summary")
    public ResponseEntity<List<ActiveHotelBillingSummary>> getActiveHotelBillingSummary(
            @RequestHeader("X-Restaurant-Id") Long restaurantId,
            @RequestParam String hotelConfigId) {
        return ResponseEntity.ok(billService.getActiveHotelBillingSummary(restaurantId, hotelConfigId));
    }

    @GetMapping("/hotel/room/{roomNumber}")
    public ResponseEntity<HotelRoomBillingResponse> getHotelRoomBilling(
            @RequestHeader("X-Restaurant-Id") Long restaurantId,
            @RequestParam String hotelConfigId,
            @PathVariable String roomNumber) {
        return ResponseEntity.ok(billService.getHotelRoomBilling(restaurantId, hotelConfigId, roomNumber));
    }

    @PostMapping("/hotel/generate")
    public ResponseEntity<UserBill> generateHotelBill(
            @RequestHeader("X-Restaurant-Id") Long restaurantId,
            @RequestBody GenerateHotelBillRequest request) {
        
        if (request == null || request.getHotelConfigId() == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            return ResponseEntity.ok(billService.generateHotelBill(restaurantId, request.getHotelConfigId(), request.getRoomNumber(), request.getUserId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/hotel/room/{roomNumber}/close")
    public ResponseEntity<Void> closeHotelRoom(
            @RequestHeader("X-Restaurant-Id") Long restaurantId,
            @RequestParam String hotelConfigId,
            @PathVariable String roomNumber) {
        billService.closeHotelRoom(restaurantId, hotelConfigId, roomNumber);
        return ResponseEntity.ok().build();
    }
}
