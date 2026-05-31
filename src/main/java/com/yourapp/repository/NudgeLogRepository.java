package com.yourapp.repository;

import com.yourapp.model.NudgeLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;

public interface NudgeLogRepository extends MongoRepository<NudgeLog, String> {

    List<NudgeLog> findByUserIdAndDate(String userId, LocalDate date);

    List<NudgeLog> findByUserIdOrderBySentAtDesc(String userId);
}
