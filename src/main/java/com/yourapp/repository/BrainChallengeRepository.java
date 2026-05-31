package com.yourapp.repository;

import com.yourapp.model.BrainChallenge;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface BrainChallengeRepository extends MongoRepository<BrainChallenge, String> {
    List<BrainChallenge> findByUserIdOrderByCreatedAtDesc(String userId);
}
