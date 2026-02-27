package com.yourapp.repository;

import com.yourapp.model.UserStreak;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserStreakRepository extends MongoRepository<UserStreak, String> {
    Optional<UserStreak> findByUserId(String userId);
}
