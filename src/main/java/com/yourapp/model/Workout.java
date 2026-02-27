package com.yourapp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "workouts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Workout {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String name;

    private WorkoutType type;

    private LocalDate workoutDate;

    private int durationMinutes;

    private double caloriesBurned;

    @Builder.Default
    private WorkoutStatus status = WorkoutStatus.DRAFT;

    @Builder.Default
    private boolean archived = false;

    @Builder.Default
    private List<String> collaboratorIds = new ArrayList<>();

    private String linkedGoalId;

    @Builder.Default
    private List<Exercise> exercises = new ArrayList<>();

    private String notes;

    /**
     * Phase 6: Pre-calculated total volume across all exercises.
     * Volume = sum(sets × reps × weightKg) per exercise.
     */
    private Double totalVolume;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Exercise {
        private String name;
        private int sets;
        private int reps;
        private double weightKg;
        private int durationSeconds;
        @Builder.Default
        private ExerciseStatus status = ExerciseStatus.PENDING;
        private String notes;
    }

    public enum WorkoutType {
        STRENGTH, CARDIO, FLEXIBILITY, HIIT, SPORT, OTHER
    }

    public enum WorkoutStatus {
        DRAFT, PENDING, IN_PROGRESS, COMPLETED, ARCHIVED
    }

    public enum ExerciseStatus {
        PENDING, IN_PROGRESS, DONE
    }
}
