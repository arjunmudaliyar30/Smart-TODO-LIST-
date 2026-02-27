package com.yourapp.repository;

import com.yourapp.model.Workout;
import com.yourapp.model.Workout.WorkoutStatus;
import com.yourapp.model.Workout.WorkoutType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkoutRepository extends MongoRepository<Workout, String> {

    List<Workout> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Workout> findByUserIdAndArchivedFalseOrderByCreatedAtDesc(String userId);

    List<Workout> findByUserIdAndArchivedTrueOrderByCreatedAtDesc(String userId);

    List<Workout> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, WorkoutStatus status);

    List<Workout> findByUserIdAndType(String userId, WorkoutType type);

    List<Workout> findByUserIdAndWorkoutDateBetween(String userId, LocalDate start, LocalDate end);

    List<Workout> findByCollaboratorIdsContaining(String userId);

    Optional<Workout> findByIdAndUserId(String id, String userId);

    void deleteByIdAndUserId(String id, String userId);

    // Phase 10 — achievement counts
    long countByUserIdAndStatus(String userId, WorkoutStatus status);
}

