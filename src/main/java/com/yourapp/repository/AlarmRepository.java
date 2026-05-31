package com.yourapp.repository;

import com.yourapp.model.Alarm;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface AlarmRepository extends MongoRepository<Alarm, String> {

    List<Alarm> findByUserIdOrderByScheduledAtAsc(String userId);

    /** Alarms that are due now, not yet fired, not dismissed. */
    @Query("{ 'userId': ?0, 'scheduledAt': { $lte: ?1 }, 'fired': false, 'dismissed': false }")
    List<Alarm> findPendingForUser(String userId, LocalDateTime now);

    /** All unfired, undismissed alarms up to now — used by the scheduler. */
    @Query("{ 'scheduledAt': { $lte: ?0 }, 'fired': false, 'dismissed': false }")
    List<Alarm> findAllPendingAlarms(LocalDateTime now);
}
