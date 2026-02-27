package com.yourapp.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoodAnalyticsDTO {

    private Double averageEnergyLevel;
    private Double averageSleepHours;

    /** Pearson-like correlation between energy level and performance score (-1 to 1). */
    private Double energyPerformanceCorrelation;

    /** True if the last 3+ data points show declining energy. */
    private boolean lowEnergyTrendDetected;

    private int totalNotesAnalyzed;

    private List<DailyMoodPoint> dailyData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyMoodPoint {
        private String date;
        private Integer energyLevel;
        private Double sleepHours;
        private String mood;
        private Double performanceScore;
    }
}
