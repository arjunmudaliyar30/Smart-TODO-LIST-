package com.yourapp.service;

import com.yourapp.dto.MoodAnalyticsDTO;
import com.yourapp.model.DailyNote;
import com.yourapp.repository.DailyNoteRepository;
import com.yourapp.repository.DailyPerformanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Phase 7: Energy & Mood Analytics
 * Computes average energy, sleep correlation with performance,
 * and low-energy trend detection.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MoodAnalyticsService {

    private final DailyNoteRepository        dailyNoteRepository;
    private final DailyPerformanceRepository dailyPerformanceRepository;

    public MoodAnalyticsDTO getAnalytics(String userId, LocalDate from, LocalDate to) {
        if (from == null) from = LocalDate.now().minusDays(30);
        if (to   == null) to   = LocalDate.now();

        List<DailyNote> notes = dailyNoteRepository
                .findByUserIdAndDeletedFalseAndDateBetweenOrderByDateDesc(userId, from, to)
                .stream()
                .filter(n -> n.getEnergyLevel() != null || n.getSleepHours() != null)
                .collect(Collectors.toList());

        if (notes.isEmpty()) {
            return MoodAnalyticsDTO.builder()
                    .totalNotesAnalyzed(0)
                    .dailyData(Collections.emptyList())
                    .build();
        }

        // Build daily data points
        List<MoodAnalyticsDTO.DailyMoodPoint> dailyData = new ArrayList<>();
        for (DailyNote note : notes) {
            Double perfScore = dailyPerformanceRepository
                    .findByUserIdAndDate(userId, note.getDate())
                    .map(dp -> dp.getScore())
                    .orElse(null);

            dailyData.add(MoodAnalyticsDTO.DailyMoodPoint.builder()
                    .date(note.getDate().toString())
                    .energyLevel(note.getEnergyLevel())
                    .sleepHours(note.getSleepHours())
                    .mood(note.getMood())
                    .performanceScore(perfScore)
                    .build());
        }

        // Averages
        OptionalDouble avgEnergy = notes.stream()
                .filter(n -> n.getEnergyLevel() != null)
                .mapToInt(DailyNote::getEnergyLevel)
                .average();
        OptionalDouble avgSleep = notes.stream()
                .filter(n -> n.getSleepHours() != null)
                .mapToDouble(DailyNote::getSleepHours)
                .average();

        // Correlation: energy vs performance
        double correlation = computeCorrelation(dailyData);

        // Low energy trend: last 3+ data points all below 5
        boolean lowEnergyTrend = detectLowEnergyTrend(notes);

        return MoodAnalyticsDTO.builder()
                .averageEnergyLevel(avgEnergy.isPresent() ? Math.round(avgEnergy.getAsDouble() * 10.0) / 10.0 : null)
                .averageSleepHours(avgSleep.isPresent() ? Math.round(avgSleep.getAsDouble() * 10.0) / 10.0 : null)
                .energyPerformanceCorrelation(Math.round(correlation * 1000.0) / 1000.0)
                .lowEnergyTrendDetected(lowEnergyTrend)
                .totalNotesAnalyzed(notes.size())
                .dailyData(dailyData)
                .build();
    }

    /** Simple Pearson correlation between energy level and performance score. */
    private double computeCorrelation(List<MoodAnalyticsDTO.DailyMoodPoint> data) {
        List<double[]> pairs = data.stream()
                .filter(d -> d.getEnergyLevel() != null && d.getPerformanceScore() != null)
                .map(d -> new double[]{d.getEnergyLevel(), d.getPerformanceScore()})
                .collect(Collectors.toList());

        if (pairs.size() < 2) return 0.0;
        double meanX = pairs.stream().mapToDouble(p -> p[0]).average().orElse(0);
        double meanY = pairs.stream().mapToDouble(p -> p[1]).average().orElse(0);
        double num   = pairs.stream().mapToDouble(p -> (p[0] - meanX) * (p[1] - meanY)).sum();
        double denX  = Math.sqrt(pairs.stream().mapToDouble(p -> Math.pow(p[0] - meanX, 2)).sum());
        double denY  = Math.sqrt(pairs.stream().mapToDouble(p -> Math.pow(p[1] - meanY, 2)).sum());
        return (denX * denY == 0) ? 0.0 : num / (denX * denY);
    }

    private boolean detectLowEnergyTrend(List<DailyNote> notes) {
        List<DailyNote> withEnergy = notes.stream()
                .filter(n -> n.getEnergyLevel() != null)
                .sorted(Comparator.comparing(DailyNote::getDate).reversed())
                .limit(3)
                .collect(Collectors.toList());
        return withEnergy.size() >= 3 && withEnergy.stream()
                .allMatch(n -> n.getEnergyLevel() < 5);
    }
}
