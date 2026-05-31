package com.yourapp.repository;

import com.yourapp.model.Habit;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface HabitRepository extends MongoRepository<Habit, String> {

    List<Habit> findByUserIdAndIsActiveTrue(String userId);

    List<Habit> findByUserId(String userId);

    Optional<Habit> findByIdAndUserId(String id, String userId);
}
