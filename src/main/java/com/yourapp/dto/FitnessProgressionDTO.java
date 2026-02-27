package com.yourapp.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FitnessProgressionDTO {

    private String exerciseName;

    /** Each data point: date + max weight used */
    private List<DataPoint> weightProgression;

    /** Total volume (sets × reps × weight) per week */
    private List<WeeklyVolume> weeklyVolume;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataPoint {
        private String date;
        private double maxWeightKg;
        private int totalSets;
        private int totalReps;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeeklyVolume {
        private String weekStart;
        private double totalVolume;
    }
}
