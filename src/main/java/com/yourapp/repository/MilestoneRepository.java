package com.yourapp.repository;

import com.yourapp.model.Milestone;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface MilestoneRepository extends MongoRepository<Milestone, String> {

    List<Milestone> findByUserIdOrderByDateAsc(String userId);

    Optional<Milestone> findByUserIdAndType(String userId, String type);

    boolean existsByUserIdAndType(String userId, String type);
}
