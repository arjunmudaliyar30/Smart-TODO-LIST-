package com.yourapp.service;

import com.yourapp.dto.PerformanceScoreDTO;
import com.yourapp.model.DailyPerformance;
import com.yourapp.model.Task;
import com.yourapp.model.Workout;
import com.yourapp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Phase 2: Performance Score Engine
 *
 * Daily Score Formula:
 *   30% Task Completion Ratio
 *   30% Workout Completion Ratio
 *   20% Calorie Discipline
 *   20% Note Written
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PerformanceScoreService {

    private final TaskRepository             taskRepository;
    private final WorkoutRepository          workoutRepository;
    private final CaloriesLogRepository      caloriesLogRepository;
    private final DailyNoteRepository        dailyNoteRepository;
    private final DailyPerformanceRepository dailyPerformanceRepository;
    private final UserFitnessProfileRepository userFitnessProfileRepository;

    /**
     * Calculates and persists the score for the given user+date.
     * Safe to call multiple times — upserts by (userId, date).
     */
    public PerformanceScoreDTO calculateAndStore(String userId, LocalDate date) {
        PerformanceScoreDTO dto = calculate(userId, date);

        // Upsert
        DailyPerformance dp = dailyPerformanceRepository
                .findByUserIdAndDate(userId, date)
                .orElseGet(() -> DailyPerformance.builder()
                        .userId(userId).date(date).build());

        dp.setScore(dto.getScore());
        dp.setTaskCompletionRatio(dto.getTaskScore() / 100.0);
        dp.setWorkoutCompletionRatio(dto.getWorkoutScore() / 100.0);
        dp.setCalorieDiscipline(dto.getCalorieScore() / 100.0);
        dp.setNoteWritten(dto.getNoteScore() / 100.0);
        dp.setRecordedAt(LocalDateTime.now());
        dailyPerformanceRepository.save(dp);

        return dto;
    }

    /** Calculates (without storing) the score for a user on a given date. */
    public PerformanceScoreDTO calculate(String userId, LocalDate date) {

        // --- Task component ---
        List<Task> allTasks = taskRepository.findByUserIdAndScheduledDate(userId, date);
        int tasksTotal     = allTasks.size();
        int tasksDone      = (int) allTasks.stream()
                .filter(t -> t.getStatus() == Task.TaskStatus.DONE
                          || t.getStatus() == Task.TaskStatus.COMPLETED)
                .count();
        double taskRatio   = tasksTotal > 0 ? (double) tasksDone / tasksTotal : 0.0;
        double taskScore   = taskRatio * 100.0;

        // --- Workout component ---
        List<Workout> workouts = workoutRepository.findByUserIdAndWorkoutDateBetween(userId, date, date);
        int workoutsTotal  = workouts.size();
        int workoutsDone   = (int) workouts.stream()
                .filter(w -> w.getStatus() == Workout.WorkoutStatus.COMPLETED)
                .count();
        double workoutRatio  = workoutsTotal > 0 ? (double) workoutsDone / workoutsTotal : 0.0;
        double workoutScore  = workoutRatio * 100.0;

        // --- Calorie component ---
        int totalConsumed  = caloriesLogRepository
                .findByUserIdAndDateOrderByCreatedAtAsc(userId, date).stream()
                .mapToInt(c -> c.getConsumed()).sum();
        int calorieGoal    = userFitnessProfileRepository.findByUserId(userId)
                .map(p -> p.getDailyCalorieGoal()).orElse(0);
        double calorieScore = 0.0;
        if (calorieGoal > 0 && totalConsumed > 0) {
            double ratio     = (double) totalConsumed / calorieGoal;
            // Ideal: consumed 80-110% of goal → full score
            if (ratio >= 0.8 && ratio <= 1.1) calorieScore = 100.0;
            else if (ratio < 0.8) calorieScore = (ratio / 0.8) * 100.0;
            else calorieScore = Math.max(0, 100.0 - (ratio - 1.1) * 200.0);
        }

        // --- Note component ---
        boolean noteWritten = dailyNoteRepository
                .existsByUserIdAndDate(userId, date);
        double noteScore    = noteWritten ? 100.0 : 0.0;

        // --- Blend ---
        double score = (taskScore * 0.30)
                     + (workoutScore * 0.30)
                     + (calorieScore * 0.20)
                     + (noteScore    * 0.20);

        return PerformanceScoreDTO.builder()
                .date(date)
                .score(Math.round(score * 10.0) / 10.0)
                .taskScore(Math.round(taskScore * 10.0) / 10.0)
                .workoutScore(Math.round(workoutScore * 10.0) / 10.0)
                .calorieScore(Math.round(calorieScore * 10.0) / 10.0)
                .noteScore(noteScore)
                .tasksTotal(tasksTotal)
                .tasksCompleted(tasksDone)
                .workoutsTotal(workoutsTotal)
                .workoutsCompleted(workoutsDone)
                .noteWritten(noteWritten)
                .calorieGoal(calorieGoal > 0 ? (double) calorieGoal : null)
                .calorieActual(totalConsumed > 0 ? (double) totalConsumed : null)
                .build();
    }
}
