package com.yourapp.repository;

import com.yourapp.model.CaloriesLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CaloriesLogRepository extends MongoRepository<CaloriesLog, String> {

    List<CaloriesLog> findByUserIdAndDateOrderByCreatedAtAsc(String userId, LocalDate date);

    List<CaloriesLog> findByUserIdAndDateBetweenOrderByDateAsc(String userId, LocalDate start, LocalDate end);

    List<CaloriesLog> findByUserIdOrderByDateDesc(String userId);

    Optional<CaloriesLog> findByIdAndUserId(String id, String userId);

    void deleteByIdAndUserId(String id, String userId);
}
