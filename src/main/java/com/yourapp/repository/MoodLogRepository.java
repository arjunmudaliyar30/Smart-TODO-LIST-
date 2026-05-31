package com.yourapp.repository;

import com.yourapp.model.MoodLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MoodLogRepository extends MongoRepository<MoodLog, String> {

    Optional<MoodLog> findByUserIdAndDate(String userId, LocalDate date);

    List<MoodLog> findByUserIdAndDateBetweenOrderByDateDesc(String userId, LocalDate from, LocalDate to);
}
