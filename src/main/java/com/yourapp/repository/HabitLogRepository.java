package com.yourapp.repository;

import com.yourapp.model.HabitLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HabitLogRepository extends MongoRepository<HabitLog, String> {

    Optional<HabitLog> findByUserIdAndHabitIdAndDate(String userId, String habitId, LocalDate date);

    List<HabitLog> findByUserIdAndDate(String userId, LocalDate date);

    List<HabitLog> findByUserIdAndDateBetween(String userId, LocalDate from, LocalDate to);

    List<HabitLog> findByUserIdAndHabitIdAndDateBetween(String userId, String habitId, LocalDate from, LocalDate to);
}
