package com.festora.captainservice.repository;

import com.festora.captainservice.model.CaptainAction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CaptainActionRepository extends MongoRepository<CaptainAction, String> {
}
