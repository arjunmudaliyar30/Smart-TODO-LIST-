package com.yourapp.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyReportDTO {

    private LocalDate weekStart;
    private LocalDate weekEnd;

    private int totalTasksCompleted;
    private int totalWorkoutsCompleted;
    private Double averageCaloriesNet;

    private LocalDate highestProductivityDay;
    private LocalDate lowestProductivityDay;
    private double highestDayScore;
    private double lowestDayScore;

    /** Longest streak (task, workout, or note) during this week. */
    private int longestStreakThisWeek;

    /** Current overall streak (max of task/workout/note streak). */
    private int currentStreak;

    /** (Days active this week / 7) × 100 */
    private double consistencyPercent;

    private List<PerformanceScoreDTO> dailyScores;
}
