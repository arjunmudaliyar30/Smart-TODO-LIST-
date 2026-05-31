package com.yourapp.repository;

import com.yourapp.model.AccountabilityScore;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AccountabilityScoreRepository extends MongoRepository<AccountabilityScore, String> {

    Optional<AccountabilityScore> findByUserIdAndDate(String userId, LocalDate date);

    List<AccountabilityScore> findByUserIdAndDateBetweenOrderByDateDesc(String userId, LocalDate from, LocalDate to);
}
