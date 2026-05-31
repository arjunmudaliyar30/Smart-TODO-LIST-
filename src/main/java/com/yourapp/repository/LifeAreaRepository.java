package com.yourapp.repository;

import com.yourapp.model.LifeArea;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface LifeAreaRepository extends MongoRepository<LifeArea, String> {

    List<LifeArea> findByUserIdOrderByCreatedAtAsc(String userId);

    Optional<LifeArea> findByIdAndUserId(String id, String userId);

    long countByUserId(String userId);
}
