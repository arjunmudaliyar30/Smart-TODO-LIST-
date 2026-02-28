package com.yourapp.service;

import com.yourapp.dto.FitnessProgressionDTO;
import com.yourapp.model.Workout;
import com.yourapp.repository.WorkoutRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Phase 6: Advanced Fitness Analytics
 * Calculates per-exercise volume and weight progression.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class VolumeCalculationService {

    private final WorkoutRepository workoutRepository;

    /**
     * Volume for a single exercise = sets × reps × weightKg.
     */
    public double calculateExerciseVolume(Workout.Exercise ex) {
        if (ex == null) return 0.0;
        return ex.getSets() * ex.getReps() * ex.getWeightKg();
    }

    /**
     * Recalculates totalVolume for a workout, sums all exercises.
     */
    public double calculateWorkoutVolume(Workout workout) {
        if (workout.getExercises() == null) return 0.0;
        return workout.getExercises().stream()
                .mapToDouble(this::calculateExerciseVolume)
                .sum();
    }

    /**
     * Returns historical weight progression and weekly total volume
     * for the given exercise name (case-insensitive match).
     */
    public FitnessProgressionDTO getExerciseProgression(String userId, String exerciseName) {
        List<Workout> workouts = workoutRepository.findByUserIdOrderByCreatedAtDesc(userId);

        String nameLower = exerciseName.toLowerCase().trim();

        List<FitnessProgressionDTO.DataPoint> weightPoints = new ArrayList<>();
        Map<String, Double> weeklyVolMap = new LinkedHashMap<>();

        for (Workout w : workouts) {
            if (w.getExercises() == null) continue;
            LocalDate wDate = w.getWorkoutDate() != null ? w.getWorkoutDate() : LocalDate.now();
            String week = wDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString();

            for (Workout.Exercise ex : w.getExercises()) {
                if (ex.getName() == null) continue;
                if (!ex.getName().toLowerCase().contains(nameLower)) continue;

                double vol    = calculateExerciseVolume(ex);
                double maxWt  = ex.getWeightKg();

                // Weight progression point
                weightPoints.add(FitnessProgressionDTO.DataPoint.builder()
                        .date(wDate.toString())
                        .maxWeightKg(maxWt)
                        .totalSets(ex.getSets())
                        .totalReps(ex.getReps())
                        .build());

                // Weekly volume
                weeklyVolMap.merge(week, vol, Double::sum);
            }
        }

        // Sort by date ascending
        weightPoints.sort(Comparator.comparing(FitnessProgressionDTO.DataPoint::getDate));

        List<FitnessProgressionDTO.WeeklyVolume> weeklyVols = weeklyVolMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> FitnessProgressionDTO.WeeklyVolume.builder()
                        .weekStart(e.getKey())
                        .totalVolume(Math.round(e.getValue() * 100.0) / 100.0)
                        .build())
                .collect(Collectors.toList());

        return FitnessProgressionDTO.builder()
                .exerciseName(exerciseName)
                .weightProgression(weightPoints)
                .weeklyVolume(weeklyVols)
                .build();
    }
}
