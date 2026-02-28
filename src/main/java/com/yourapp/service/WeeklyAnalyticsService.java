package com.yourapp.service;

import com.yourapp.dto.PerformanceScoreDTO;
import com.yourapp.dto.WeeklyReportDTO;
import com.yourapp.model.UserStreak;
import com.yourapp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * Phase 3: Weekly Report Engine
 * Generates a dynamic weekly analytics report — not stored permanently.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeeklyAnalyticsService {

    private final PerformanceScoreService performanceScoreService;
    private final CaloriesLogRepository   caloriesLogRepository;
    private final UserStreakRepository    userStreakRepository;

    /**
     * Generates the weekly report for the week containing the given date
     * (Monday–Sunday). Defaults to current week if date is null.
     */
    public WeeklyReportDTO generateWeeklyReport(String userId, LocalDate refDate) {
        if (refDate == null) refDate = LocalDate.now();

        LocalDate weekStart = refDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd   = weekStart.plusDays(6);

        // Daily scores for each day of the week
        List<PerformanceScoreDTO> dailyScores = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = weekStart.plusDays(i);
            dailyScores.add(performanceScoreService.calculate(userId, day));
        }

        // Best/worst days
        PerformanceScoreDTO best  = dailyScores.stream().max(Comparator.comparingDouble(PerformanceScoreDTO::getScore)).orElse(null);
        PerformanceScoreDTO worst = dailyScores.stream().min(Comparator.comparingDouble(PerformanceScoreDTO::getScore)).orElse(null);

        // Totals
        int totalTasksDone = dailyScores.stream()
                .mapToInt(PerformanceScoreDTO::getTasksCompleted).sum();
        int totalWorkoutsDone = dailyScores.stream()
                .mapToInt(PerformanceScoreDTO::getWorkoutsCompleted).sum();

        // Average calorie net (consumed - burned)
        List<com.yourapp.model.CaloriesLog> calLogs =
                caloriesLogRepository.findByUserIdAndDateBetweenOrderByDateAsc(userId, weekStart, weekEnd);
        OptionalDouble avgNet = calLogs.stream()
                .mapToDouble(c -> c.getConsumed() - c.getBurned())
                .average();

        // Consistency = active days / 7
        long activeDays = dailyScores.stream()
                .filter(d -> d.getTasksCompleted() > 0
                          || d.getWorkoutsCompleted() > 0
                          || d.isNoteWritten())
                .count();
        double consistencyPercent = (activeDays / 7.0) * 100.0;

        // Current streaks
        UserStreak streakDoc = userStreakRepository.findByUserId(userId)
                .orElse(UserStreak.builder().build());
        int currentStreak = Math.max(Math.max(streakDoc.getTaskStreak(), streakDoc.getWorkoutStreak()),
                Math.max(streakDoc.getCalorieStreak(), streakDoc.getNoteStreak()));

        // Longest streak across this week (simple approximation: max consecutive active days)
        int longestThisWeek = longestConsecutiveActiveDays(dailyScores);

        return WeeklyReportDTO.builder()
                .weekStart(weekStart)
                .weekEnd(weekEnd)
                .totalTasksCompleted(totalTasksDone)
                .totalWorkoutsCompleted(totalWorkoutsDone)
                .averageCaloriesNet(avgNet.isPresent() ? Math.round(avgNet.getAsDouble() * 10.0) / 10.0 : null)
                .highestProductivityDay(best  != null ? best.getDate()  : null)
                .lowestProductivityDay( worst != null ? worst.getDate() : null)
                .highestDayScore(best  != null ? best.getScore()  : 0)
                .lowestDayScore( worst != null ? worst.getScore() : 0)
                .longestStreakThisWeek(longestThisWeek)
                .currentStreak(currentStreak)
                .consistencyPercent(Math.round(consistencyPercent * 10.0) / 10.0)
                .dailyScores(dailyScores)
                .build();
    }

    private int longestConsecutiveActiveDays(List<PerformanceScoreDTO> scores) {
        int max = 0, cur = 0;
        for (PerformanceScoreDTO s : scores) {
            boolean active = s.getTasksCompleted() > 0
                          || s.getWorkoutsCompleted() > 0
                          || s.isNoteWritten();
            cur = active ? cur + 1 : 0;
            max = Math.max(max, cur);
        }
        return max;
    }
}
