package com.yourapp.repository;

import com.yourapp.model.MorningBriefing;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MorningBriefingRepository extends MongoRepository<MorningBriefing, String> {

    Optional<MorningBriefing> findByUserIdAndDate(String userId, LocalDate date);

    List<MorningBriefing> findByUserIdOrderByDateDesc(String userId);
}
