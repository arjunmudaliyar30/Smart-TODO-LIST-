package com.yourapp.service;

import com.yourapp.dto.WorkoutScheduleRequest;
import com.yourapp.model.Workout;
import com.yourapp.model.WorkoutSchedule;
import com.yourapp.model.WorkoutSchedule.DayPlan;
import com.yourapp.model.WorkoutSchedule.PlannedExercise;
import com.yourapp.model.WorkoutSchedule.PlannedWorkout;
import com.yourapp.repository.WorkoutRepository;
import com.yourapp.repository.WorkoutScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkoutScheduleService {

    private final WorkoutScheduleRepository scheduleRepo;
    private final WorkoutRepository         workoutRepo;

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Returns the Monday of the ISO week that contains the given date. */
    public static LocalDate toMonday(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    // -----------------------------------------------------------------------
    // GET — return existing or empty scaffold
    // -----------------------------------------------------------------------

    public WorkoutSchedule getSchedule(String userId, LocalDate weekStartDate) {
        LocalDate monday = toMonday(weekStartDate);
        return scheduleRepo.findByUserIdAndWeekStartDate(userId, monday)
                .orElseGet(() -> buildEmptySchedule(userId, monday));
    }

    private WorkoutSchedule buildEmptySchedule(String userId, LocalDate monday) {
        List<DayPlan> days = new ArrayList<>();
        String[] names = {"MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY"};
        for (int i = 0; i < 7; i++) {
            days.add(DayPlan.builder()
                    .dayOfWeek(names[i])
                    .date(monday.plusDays(i))
                    .workouts(new ArrayList<>())
                    .build());
        }
        return WorkoutSchedule.builder()
                .userId(userId)
                .weekStartDate(monday)
                .days(days)
                .build();
    }

    // -----------------------------------------------------------------------
    // SAVE — upsert full week plan
    // -----------------------------------------------------------------------

    public WorkoutSchedule saveSchedule(String userId, WorkoutScheduleRequest req) {
        LocalDate monday = toMonday(req.getWeekStartDate());

        // Ensure each day has a planId and correct date
        List<DayPlan> days = req.getDays() == null ? new ArrayList<>() : req.getDays();
        String[] names = {"MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY"};
        for (int i = 0; i < days.size(); i++) {
            DayPlan day = days.get(i);
            if (day.getDate() == null) day.setDate(monday.plusDays(i));
            if (day.getWorkouts() != null) {
                for (PlannedWorkout pw : day.getWorkouts()) {
                    if (pw.getPlanId() == null || pw.getPlanId().isBlank()) {
                        pw.setPlanId(UUID.randomUUID().toString());
                    }
                }
            }
        }

        Optional<WorkoutSchedule> existing = scheduleRepo.findByUserIdAndWeekStartDate(userId, monday);
        WorkoutSchedule schedule;
        if (existing.isPresent()) {
            schedule = existing.get();
            schedule.setDays(days);
            schedule.setUpdatedAt(LocalDateTime.now());
        } else {
            schedule = WorkoutSchedule.builder()
                    .userId(userId)
                    .weekStartDate(monday)
                    .days(days)
                    .updatedAt(LocalDateTime.now())
                    .build();
        }
        return scheduleRepo.save(schedule);
    }

    // -----------------------------------------------------------------------
    // PUSH — convert planned workouts to real Workout documents
    // -----------------------------------------------------------------------

    /**
     * For each PlannedWorkout in the week, create a real Workout document
     * (if one with the same name + date doesn't already exist for the user).
     * Returns how many were created.
     */
    public int pushToActiveWorkouts(String userId, LocalDate weekStartDate) {
        LocalDate monday = toMonday(weekStartDate);
        WorkoutSchedule sched = scheduleRepo.findByUserIdAndWeekStartDate(userId, monday)
                .orElseThrow(() -> new RuntimeException("No schedule found for that week"));

        int created = 0;
        for (DayPlan day : sched.getDays()) {
            if (day.getWorkouts() == null) continue;
            for (PlannedWorkout pw : day.getWorkouts()) {
                // Avoid duplicates: check if a workout with same name+date already exists
                boolean exists = workoutRepo.findByUserIdOrderByCreatedAtDesc(userId)
                        .stream()
                        .anyMatch(w -> w.getName() != null
                                && w.getName().equalsIgnoreCase(pw.getName())
                                && day.getDate() != null
                                && day.getDate().equals(w.getWorkoutDate()));
                if (exists) continue;

                // Build exercises
                List<Workout.Exercise> exercises = new ArrayList<>();
                if (pw.getExercises() != null) {
                    for (PlannedExercise pe : pw.getExercises()) {
                        exercises.add(Workout.Exercise.builder()
                                .name(pe.getName())
                                .sets(pe.getSets() != null ? pe.getSets() : 0)
                                .reps(pe.getReps() != null ? pe.getReps() : 0)
                                .weightKg(pe.getWeightKg() != null ? pe.getWeightKg() : 0)
                                .notes(pe.getNotes())
                                .status(Workout.ExerciseStatus.PENDING)
                                .build());
                    }
                }

                Workout workout = Workout.builder()
                        .userId(userId)
                        .name(pw.getName())
                        .workoutDate(day.getDate())
                        .durationMinutes(pw.getDurationMinutes() != null ? pw.getDurationMinutes() : 0)
                        .status(Workout.WorkoutStatus.PENDING)
                        .exercises(exercises)
                        .notes(pw.getNotes())
                        .build();

                workoutRepo.save(workout);
                created++;
            }
        }
        log.info("pushToActiveWorkouts: userId={} week={} created={}", userId, monday, created);
        return created;
    }

    // -----------------------------------------------------------------------
    // GET ALL schedules for user (for monthly view)
    // -----------------------------------------------------------------------

    public List<WorkoutSchedule> getAllSchedules(String userId) {
        return scheduleRepo.findByUserIdOrderByWeekStartDateDesc(userId);
    }
}
