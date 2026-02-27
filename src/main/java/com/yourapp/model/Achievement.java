package com.yourapp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Template for an achievement badge.
 * Pre-seeded on application startup by AchievementService.
 */
@Document(collection = "achievements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Achievement {

    @Id
    private String id;

    private String name;
    private String description;
    private String badgeIcon;

    private CriteriaType criteriaType;
    private int criteriaValue;

    public enum CriteriaType {
        TASK_COUNT,
        WORKOUT_COUNT,
        NOTE_STREAK,
        TASK_STREAK,
        WORKOUT_STREAK,
        CALORIE_STREAK,
        FOCUS_MINUTES,
        GOAL_COMPLETED
    }
}
