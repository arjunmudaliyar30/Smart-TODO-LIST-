package com.yourapp.repository;

import com.yourapp.model.EveningReflection;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EveningReflectionRepository extends MongoRepository<EveningReflection, String> {

    Optional<EveningReflection> findByUserIdAndDate(String userId, LocalDate date);

    List<EveningReflection> findByUserIdOrderByDateDesc(String userId);

    List<EveningReflection> findByUserIdAndDateBetweenOrderByDateDesc(String userId, LocalDate from, LocalDate to);

    long countByUserId(String userId);
}
