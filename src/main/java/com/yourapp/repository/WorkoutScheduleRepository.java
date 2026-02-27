package com.yourapp.repository;

import com.yourapp.model.WorkoutSchedule;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkoutScheduleRepository extends MongoRepository<WorkoutSchedule, String> {

    Optional<WorkoutSchedule> findByUserIdAndWeekStartDate(String userId, LocalDate weekStartDate);

    List<WorkoutSchedule> findByUserIdOrderByWeekStartDateDesc(String userId);
}
