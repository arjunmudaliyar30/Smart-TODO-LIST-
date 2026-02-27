package com.yourapp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

@Document(collection = "user_fitness_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFitnessProfile {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    private int age;

    private double weightKg;

    private double heightCm;

    /** "M" or "F" */
    private String gender;

    private ActivityLevel activityLevel;

    /**
     * Daily calorie goal — can be manually overridden or computed via
     * Mifflin-St Jeor formula on the frontend.
     */
    private int dailyCalorieGoal;

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum ActivityLevel {
        SEDENTARY, LIGHT, MODERATE, ACTIVE, VERY_ACTIVE
    }
}
