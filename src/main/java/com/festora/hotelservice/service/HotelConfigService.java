package com.festora.hotelservice.service;

import com.festora.hotelservice.dto.CreateHotelRequest;
import com.festora.hotelservice.model.HotelConfig;
import com.festora.hotelservice.repository.HotelConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class HotelConfigService {

    private final HotelConfigRepository hotelConfigRepository;

    @Value("${app.frontend.hotel.url:${app.frontend.url}}")
    private String frontendHotelUrl;

    public HotelConfig createHotel(Long restaurantId, CreateHotelRequest request) {
        if (restaurantId == null) {
            throw new IllegalArgumentException("restaurantId is required");
        }
        if (request.getHotelName() == null || request.getHotelName().isBlank()) {
            throw new IllegalArgumentException("Hotel name is required");
        }
        if (request.getOwnerName() == null || request.getOwnerName().isBlank()) {
            throw new IllegalArgumentException("Owner name is required");
        }
        if (request.getMobile() == null || request.getMobile().isBlank()) {
            throw new IllegalArgumentException("Mobile number is required");
        }

        HotelConfig config = HotelConfig.builder()
                .restaurantId(restaurantId)
                .hotelName(request.getHotelName())
                .hotelType(request.getHotelType() != null ? request.getHotelType() : "HOTEL")
                .ownerName(request.getOwnerName())
                .mobile(request.getMobile())
                .email(request.getEmail())
                .address(request.getAddress())
                .city(request.getCity())
                .totalRooms(request.getTotalRooms())
                .roomValidationEnabled(request.isRoomValidationEnabled())
                .qrId(UUID.randomUUID().toString())
                .active(true)
                .createdAt(System.currentTimeMillis())
                .build();

        HotelConfig saved = hotelConfigRepository.save(config);
        log.info("Created hotel config: {} for restaurant: {}", saved.getId(), restaurantId);
        return saved;
    }

    public List<HotelConfig> getHotelsByRestaurant(Long restaurantId) {
        return hotelConfigRepository.findByRestaurantId(restaurantId);
    }

    public List<HotelConfig> getActiveHotelsByRestaurant(Long restaurantId) {
        return hotelConfigRepository.findByRestaurantIdAndActive(restaurantId, true);
    }

    public HotelConfig getById(String hotelConfigId) {
        return hotelConfigRepository.findById(hotelConfigId)
                .orElseThrow(() -> new IllegalArgumentException("Hotel not found: " + hotelConfigId));
    }

    public HotelConfig getByQrId(String qrId) {
        return hotelConfigRepository.findByQrId(qrId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid hotel QR: " + qrId));
    }

    public HotelConfig updateHotel(String hotelConfigId, CreateHotelRequest request) {
        HotelConfig config = getById(hotelConfigId);

        if (request.getHotelName() != null) config.setHotelName(request.getHotelName());
        if (request.getHotelType() != null) config.setHotelType(request.getHotelType());
        if (request.getOwnerName() != null) config.setOwnerName(request.getOwnerName());
        if (request.getMobile() != null) config.setMobile(request.getMobile());
        if (request.getEmail() != null) config.setEmail(request.getEmail());
        if (request.getAddress() != null) config.setAddress(request.getAddress());
        if (request.getCity() != null) config.setCity(request.getCity());
        if (request.getTotalRooms() != null) config.setTotalRooms(request.getTotalRooms());
        config.setRoomValidationEnabled(request.isRoomValidationEnabled());

        return hotelConfigRepository.save(config);
    }

    public HotelConfig toggleActive(String hotelConfigId) {
        HotelConfig config = getById(hotelConfigId);
        config.setActive(!Boolean.TRUE.equals(config.getActive()));
        return hotelConfigRepository.save(config);
    }

    public String getHotelQrUrl(String hotelConfigId) {
        HotelConfig config = getById(hotelConfigId);
        return frontendHotelUrl + URLEncoder.encode(config.getQrId(), StandardCharsets.UTF_8);
    }

    public void deleteHotel(String hotelConfigId) {
        HotelConfig config = getById(hotelConfigId);
        hotelConfigRepository.delete(config);
        log.info("Deleted hotel config: {}", hotelConfigId);
    }
}
