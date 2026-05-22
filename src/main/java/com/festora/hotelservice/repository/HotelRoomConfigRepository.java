package com.festora.hotelservice.repository;

import com.festora.hotelservice.model.HotelRoomConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HotelRoomConfigRepository extends MongoRepository<HotelRoomConfig, String> {

    List<HotelRoomConfig> findByHotelConfigIdOrderByFloorNumberAsc(String hotelConfigId);

    void deleteByHotelConfigId(String hotelConfigId);
}
