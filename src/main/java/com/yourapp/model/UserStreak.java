package com.yourapp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Tracks consecutive-day streaks per user per activity type.
 * One document per user, updated on each qualifying event.
 */
@Document(collection = "user_streaks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStreak {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    @Builder.Default private int taskStreak    = 0;
    @Builder.Default private int workoutStreak = 0;
    @Builder.Default private int calorieStreak = 0;
    @Builder.Default private int noteStreak    = 0;

    private LocalDate lastTaskDate;
    private LocalDate lastWorkoutDate;
    private LocalDate lastCalorieDate;
    private LocalDate lastNoteDate;

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
