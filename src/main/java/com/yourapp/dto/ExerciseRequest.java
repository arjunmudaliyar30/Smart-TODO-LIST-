package com.yourapp.dto;

import com.yourapp.model.Workout.ExerciseStatus;
import lombok.Data;

@Data
public class ExerciseRequest {

    private String name;

    private int sets;

    private int reps;

    private double weightKg;

    private int durationSeconds;

    private ExerciseStatus status;

    private String notes;
}
