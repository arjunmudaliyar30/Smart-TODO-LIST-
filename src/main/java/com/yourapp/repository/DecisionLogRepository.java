package com.yourapp.repository;

import com.yourapp.model.DecisionLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DecisionLogRepository extends MongoRepository<DecisionLog, String> {
    List<DecisionLog> findByUserIdOrderByDecisionDateDescCreatedAtDesc(String userId);
}
