package com.festora.hotelservice.controller;

import com.festora.hotelservice.dto.AddFloorRequest;
import com.festora.hotelservice.model.HotelRoomConfig;
import com.festora.hotelservice.service.HotelRoomConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth/hotel/rooms")
@RequiredArgsConstructor
public class HotelRoomController {

    private final HotelRoomConfigService roomConfigService;

    @PostMapping("/{hotelId}/floors")
    public ResponseEntity<?> addFloor(
            @PathVariable String hotelId,
            @RequestBody AddFloorRequest request) {
        try {
            HotelRoomConfig floor = roomConfigService.addFloor(hotelId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(floor);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{hotelId}/floors")
    public ResponseEntity<List<HotelRoomConfig>> getFloors(@PathVariable String hotelId) {
        return ResponseEntity.ok(roomConfigService.getFloors(hotelId));
    }

    @DeleteMapping("/floors/{floorId}")
    public ResponseEntity<Void> deleteFloor(@PathVariable String floorId) {
        roomConfigService.deleteFloor(floorId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{hotelId}/validate/{roomNumber}")
    public ResponseEntity<Map<String, Object>> validateRoom(
            @PathVariable String hotelId,
            @PathVariable String roomNumber) {
        boolean valid = roomConfigService.validateRoom(hotelId, roomNumber);
        return ResponseEntity.ok(Map.of("valid", valid, "roomNumber", roomNumber));
    }

    @GetMapping("/{hotelId}/rooms/all")
    public ResponseEntity<List<String>> getAllRooms(@PathVariable String hotelId) {
        return ResponseEntity.ok(roomConfigService.getAllValidRooms(hotelId));
    }
}