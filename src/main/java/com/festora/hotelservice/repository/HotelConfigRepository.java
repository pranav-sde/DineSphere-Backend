package com.festora.hotelservice.repository;

import com.festora.hotelservice.model.HotelConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HotelConfigRepository extends MongoRepository<HotelConfig, String> {

    List<HotelConfig> findByRestaurantId(Long restaurantId);

    List<HotelConfig> findByRestaurantIdAndActive(Long restaurantId, Boolean active);

    Optional<HotelConfig> findByQrId(String qrId);

    long countByRestaurantId(Long restaurantId);
}
