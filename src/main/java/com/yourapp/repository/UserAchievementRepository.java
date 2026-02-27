package com.yourapp.repository;

import com.yourapp.model.UserAchievement;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAchievementRepository extends MongoRepository<UserAchievement, String> {
    List<UserAchievement> findByUserId(String userId);
    Optional<UserAchievement> findByUserIdAndAchievementId(String userId, String achievementId);
    boolean existsByUserIdAndAchievementId(String userId, String achievementId);
    long countByUserId(String userId);
}
