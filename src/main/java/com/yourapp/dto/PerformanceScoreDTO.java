package com.yourapp.dto;

import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceScoreDTO {

    private LocalDate date;

    /** Final blended score 0–100. */
    private double score;

    /** Task completion ratio component (0–100). */
    private double taskScore;

    /** Workout completion ratio component (0–100). */
    private double workoutScore;

    /** Calorie discipline component (0–100). */
    private double calorieScore;

    /** Note written component (0 or 100). */
    private double noteScore;

    // Raw inputs for transparency
    private int tasksTotal;
    private int tasksCompleted;
    private int workoutsTotal;
    private int workoutsCompleted;
    private boolean noteWritten;
    private Double calorieGoal;
    private Double calorieActual;
}
