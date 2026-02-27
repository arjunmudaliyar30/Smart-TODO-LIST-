package com.yourapp.dto;

import com.yourapp.model.Workout.Exercise;
import com.yourapp.model.Workout.WorkoutStatus;
import com.yourapp.model.Workout.WorkoutType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class WorkoutRequest {

    @NotBlank(message = "Workout name is required")
    private String name;

    /** Optional — null is fine for draft workouts */
    private WorkoutType type;

    /** Optional — null is fine for draft workouts */
    private LocalDate workoutDate;

    private int durationMinutes;

    private double caloriesBurned;

    private WorkoutStatus status;

    private String linkedGoalId;

    private List<String> collaboratorIds;

    private List<Exercise> exercises;

    private String notes;
}

