package com.yourapp.repository;

import com.yourapp.model.FocusSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FocusSessionRepository extends MongoRepository<FocusSession, String> {

    List<FocusSession> findByUserId(String userId);

    /** Find active (non-completed, non-expired) sessions whose planned endTime has passed. */
    List<FocusSession> findByCompletedFalseAndExpiredFalseAndEndTimeBefore(LocalDateTime threshold);

    List<FocusSession> findByUserIdAndCompletedTrue(String userId);
}
