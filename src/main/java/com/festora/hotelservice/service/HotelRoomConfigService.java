package com.festora.hotelservice.service;

import com.festora.hotelservice.dto.AddFloorRequest;
import com.festora.hotelservice.model.HotelConfig;
import com.festora.hotelservice.model.HotelRoomConfig;
import com.festora.hotelservice.repository.HotelRoomConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HotelRoomConfigService {

    private final HotelRoomConfigRepository roomConfigRepository;
    private final HotelConfigService hotelConfigService;

    public HotelRoomConfig addFloor(String hotelConfigId, AddFloorRequest request) {
        // Verify hotel exists
        HotelConfig hotelConfig = hotelConfigService.getById(hotelConfigId);
        if (ObjectUtils.isEmpty(hotelConfig))
            throw new IllegalArgumentException("Hotel is Not Registered");

        int floorNum = request.getFloorNumber();
        if (floorNum == 0) {
            List<HotelRoomConfig> existing = roomConfigRepository.findByHotelConfigIdOrderByFloorNumberAsc(hotelConfigId);
            floorNum = existing.isEmpty() ? 0 : existing.get(existing.size() - 1).getFloorNumber() + 1;
        }

        String prefix = request.getRoomPrefix() != null ? request.getRoomPrefix() : "";
        List<String> roomNumbers = new ArrayList<>();
        for(int i = request.getStartRoom(); i <= request.getEndRoom(); i++) {
            roomNumbers.add(prefix + i);
        }

        request.setRoomNumbers(roomNumbers);

        if (request.getRoomNumbers() == null || request.getRoomNumbers().isEmpty()) {
            throw new IllegalArgumentException("Room numbers list cannot be empty");
        }

        HotelRoomConfig config = HotelRoomConfig.builder()
                .hotelConfigId(hotelConfigId)
                .floorLabel(request.getFloorLabel())
                .floorNumber(floorNum)
                .roomPrefix(request.getRoomPrefix())
                .startRoom(request.getStartRoom())
                .endRoom(request.getEndRoom())
                .roomNumbers(request.getRoomNumbers())
                .build();

        HotelRoomConfig saved = roomConfigRepository.save(config);
        log.info("Added floor '{}' with {} rooms to hotel {}", 
                request.getFloorLabel(), request.getRoomNumbers().size(), hotelConfigId);
        return saved;
    }

    public List<HotelRoomConfig> getFloors(String hotelConfigId) {
        return roomConfigRepository.findByHotelConfigIdOrderByFloorNumberAsc(hotelConfigId);
    }

    public void deleteFloor(String floorId) {
        roomConfigRepository.deleteById(floorId);
    }

    /**
     * Validates if a room number exists in the hotel's configured rooms.
     * Returns true if validation is disabled (free text mode) or room is found.
     */
    public boolean validateRoom(String hotelConfigId, String roomNumber) {
        HotelConfig hotel = hotelConfigService.getById(hotelConfigId);

        // If validation is disabled, accept any room number
        if (!hotel.isRoomValidationEnabled()) {
            return true;
        }

        List<HotelRoomConfig> floors = getFloors(hotelConfigId);
        return floors.stream()
                .flatMap(floor -> floor.getRoomNumbers().stream())
                .anyMatch(room -> room.equalsIgnoreCase(roomNumber.trim()));
    }

    /**
     * Returns all valid room numbers for a hotel, ordered by floor.
     * Used by frontend for autocomplete/dropdown when validation is enabled.
     */
    public List<String> getAllValidRooms(String hotelConfigId) {
        return getFloors(hotelConfigId).stream()
                .flatMap(floor -> floor.getRoomNumbers().stream())
                .toList();
    }
}
