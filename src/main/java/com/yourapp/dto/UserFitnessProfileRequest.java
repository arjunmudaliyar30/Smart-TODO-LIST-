package com.yourapp.dto;

import com.yourapp.model.UserFitnessProfile.ActivityLevel;
import lombok.Data;

@Data
public class UserFitnessProfileRequest {

    private int age;

    private double weightKg;

    private double heightCm;

    /** "M" or "F" */
    private String gender;

    private ActivityLevel activityLevel;

    /** Manual override of computed daily calorie goal (0 = auto-compute) */
    private int dailyCalorieGoal;
}
