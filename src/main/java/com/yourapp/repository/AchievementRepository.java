package com.yourapp.repository;

import com.yourapp.model.Achievement;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AchievementRepository extends MongoRepository<Achievement, String> {
    Optional<Achievement> findByName(String name);
    List<Achievement> findByCriteriaType(Achievement.CriteriaType type);
}
