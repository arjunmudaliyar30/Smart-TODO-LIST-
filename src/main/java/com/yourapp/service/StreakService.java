package com.yourapp.service;

import com.yourapp.dto.StreakDTO;
import com.yourapp.model.UserStreak;
import com.yourapp.repository.UserStreakRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Phase 1: Streak Engine
 * Manages consecutive-day streaks for tasks, workouts, calories, and notes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class StreakService {

    private final UserStreakRepository userStreakRepository;

    public UserStreak getOrCreate(String userId) {
        return userStreakRepository.findByUserId(userId)
                .orElseGet(() -> userStreakRepository.save(
                        UserStreak.builder().userId(userId).build()));
    }

    public StreakDTO getStreakDTO(String userId) {
        UserStreak s = getOrCreate(userId);
        return StreakDTO.builder()
                .taskStreak(s.getTaskStreak())
                .workoutStreak(s.getWorkoutStreak())
                .calorieStreak(s.getCalorieStreak())
                .noteStreak(s.getNoteStreak())
                .maxStreak(Math.max(Math.max(s.getTaskStreak(), s.getWorkoutStreak()),
                        Math.max(s.getCalorieStreak(), s.getNoteStreak())))
                .lastTaskDate(s.getLastTaskDate())
                .lastWorkoutDate(s.getLastWorkoutDate())
                .lastCalorieDate(s.getLastCalorieDate())
                .lastNoteDate(s.getLastNoteDate())
                .build();
    }

    /** Called when a task is completed. Updates task streak. */
    public void recordTaskCompleted(String userId, LocalDate date) {
        UserStreak streak = getOrCreate(userId);
        streak.setTaskStreak(computeNewStreak(streak.getTaskStreak(), streak.getLastTaskDate(), date));
        streak.setLastTaskDate(date);
        streak.setUpdatedAt(LocalDateTime.now());
        userStreakRepository.save(streak);
        log.debug("Task streak for user {} → {}", userId, streak.getTaskStreak());
    }

    /** Called when a workout is completed. */
    public void recordWorkoutCompleted(String userId, LocalDate date) {
        UserStreak streak = getOrCreate(userId);
        streak.setWorkoutStreak(computeNewStreak(streak.getWorkoutStreak(), streak.getLastWorkoutDate(), date));
        streak.setLastWorkoutDate(date);
        streak.setUpdatedAt(LocalDateTime.now());
        userStreakRepository.save(streak);
    }

    /** Called when calories are logged within goal. */
    public void recordCaloriesLogged(String userId, LocalDate date, boolean withinGoal) {
        if (!withinGoal) return; // Only streak if within goal
        UserStreak streak = getOrCreate(userId);
        streak.setCalorieStreak(computeNewStreak(streak.getCalorieStreak(), streak.getLastCalorieDate(), date));
        streak.setLastCalorieDate(date);
        streak.setUpdatedAt(LocalDateTime.now());
        userStreakRepository.save(streak);
    }

    /** Called when a daily note is written. */
    public void recordNoteWritten(String userId, LocalDate date) {
        UserStreak streak = getOrCreate(userId);
        streak.setNoteStreak(computeNewStreak(streak.getNoteStreak(), streak.getLastNoteDate(), date));
        streak.setLastNoteDate(date);
        streak.setUpdatedAt(LocalDateTime.now());
        userStreakRepository.save(streak);
    }

    /**
     * Streak rules:
     * - If last date was yesterday → increment
     * - If last date is today → no change (already counted today)
     * - If gap > 1 day (or no previous) → reset to 1
     */
    private int computeNewStreak(int current, LocalDate lastDate, LocalDate today) {
        if (lastDate == null) return 1;
        long gap = ChronoUnit.DAYS.between(lastDate, today);
        if (gap == 0) return current;        // same day, no change
        if (gap == 1) return current + 1;    // consecutive day
        return 1;                             // streak broken
    }
}
