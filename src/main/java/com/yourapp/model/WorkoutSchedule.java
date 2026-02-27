package com.yourapp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores a user's weekly exercise schedule.
 * One document per user per week (keyed by userId + weekStartDate, always Monday).
 */
@Document(collection = "workout_schedules")
@CompoundIndex(name = "user_week_idx", def = "{'userId': 1, 'weekStartDate': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutSchedule {

    @Id
    private String id;

    private String userId;

    /** The Monday of the scheduled week (ISO format). */
    private LocalDate weekStartDate;

    /** 7 day-plans (Mon–Sun). Can be fewer if only some days are planned. */
    @Builder.Default
    private List<DayPlan> days = new ArrayList<>();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // -----------------------------------------------------------------------
    // Inner classes
    // -----------------------------------------------------------------------

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayPlan {
        /** Full name: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY */
        private String dayOfWeek;
        /** Concrete date for this day (derived from weekStartDate + offset). */
        private LocalDate date;
        @Builder.Default
        private List<PlannedWorkout> workouts = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlannedWorkout {
        /** Client-generated UUID so the UI can identify/edit individual planned entries. */
        private String planId;
        private String name;
        /** Matches Workout.WorkoutType enum string. */
        private String type;
        private Integer durationMinutes;
        @Builder.Default
        private List<PlannedExercise> exercises = new ArrayList<>();
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlannedExercise {
        private String name;
        private Integer sets;
        private Integer reps;
        private Double  weightKg;
        private String  notes;
    }
}
