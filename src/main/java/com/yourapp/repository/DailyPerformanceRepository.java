package com.yourapp.repository;

import com.yourapp.model.DailyPerformance;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyPerformanceRepository extends MongoRepository<DailyPerformance, String> {

    Optional<DailyPerformance> findByUserIdAndDate(String userId, LocalDate date);

    List<DailyPerformance> findByUserIdAndDateBetween(String userId, LocalDate from, LocalDate to);

    List<DailyPerformance> findByUserIdOrderByDateDesc(String userId);
}
