package com.yourapp.dto;

import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreakDTO {
    private int taskStreak;
    private int workoutStreak;
    private int calorieStreak;
    private int noteStreak;
    private int maxStreak;
    private LocalDate lastTaskDate;
    private LocalDate lastWorkoutDate;
    private LocalDate lastCalorieDate;
    private LocalDate lastNoteDate;
}
