package com.festora.captainservice.repository;

import com.festora.captainservice.model.TableZone;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TableZoneRepository extends MongoRepository<TableZone, String> {
}
