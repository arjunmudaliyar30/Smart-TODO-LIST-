package com.yourapp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Records the date a user earned a specific achievement.
 * Unique per (userId, achievementId) — cannot earn the same badge twice.
 */
@Document(collection = "user_achievements")
@CompoundIndex(name = "user_achievement_unique", def = "{'userId': 1, 'achievementId': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAchievement {

    @Id
    private String id;

    private String userId;
    private String achievementId;
    private LocalDate achievedDate;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
