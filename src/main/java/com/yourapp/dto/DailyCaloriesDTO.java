package com.yourapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Summary of calorie data for a single day.
 * net = totalConsumed - totalBurned (clamped at 0 if no records).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyCaloriesDTO {

    private LocalDate date;

    /** Sum of caloriesConsumed across all tasks on this date. */
    private int totalConsumed;

    /** Sum of caloriesBurned across all tasks on this date. */
    private int totalBurned;

    /** Net = totalConsumed - totalBurned. */
    private int net;
}
