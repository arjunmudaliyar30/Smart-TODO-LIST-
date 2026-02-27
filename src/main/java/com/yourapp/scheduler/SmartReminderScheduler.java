package com.yourapp.scheduler;

import com.yourapp.model.Task;
import com.yourapp.model.User;
import com.yourapp.model.Workout;
import com.yourapp.repository.*;
import com.yourapp.service.FocusSessionService;
import com.yourapp.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Phase 4: Smart Reminder Scheduler
 *
 * Runs every 15 minutes and checks:
 * 1. No workout logged by 7 PM → remind user to work out
 * 2. Calorie intake > 120% of goal → alert
 * 3. Auto-complete expired focus sessions
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SmartReminderScheduler {

    private final UserRepository               userRepository;
    private final WorkoutRepository            workoutRepository;
    private final CaloriesLogRepository        caloriesLogRepository;
    private final UserFitnessProfileRepository userFitnessProfileRepository;
    private final NotificationService          notificationService;
    private final FocusSessionService          focusSessionService;

    /** Runs every 15 minutes */
    @Scheduled(fixedRate = 900_000)
    public void runSmartReminders() {
        log.debug("Running smart reminders…");
        LocalDate today = LocalDate.now();
        LocalTime now   = LocalTime.now();

        List<User> users = userRepository.findAll();

        for (User user : users) {
            try {
                checkWorkoutReminder(user, today, now);
                checkCalorieOverage(user, today);
            } catch (Exception e) {
                log.warn("SmartReminder error for user {}: {}", user.getId(), e.getMessage());
            }
        }

        // Auto-complete focus sessions
        focusSessionService.processExpiredSessions();
    }

    /** If it's past 7 PM and no workout today → remind */
    private void checkWorkoutReminder(User user, LocalDate today, LocalTime now) {
        if (now.isBefore(LocalTime.of(19, 0))) return;

        boolean hasWorkoutToday = workoutRepository
                .findByUserIdAndWorkoutDateBetween(user.getId(), today, today)
                .stream()
                .anyMatch(w -> w.getStatus() == Workout.WorkoutStatus.COMPLETED);

        if (!hasWorkoutToday) {
            notificationService.sendNotification(user,
                    "💪 No workout logged today yet! Still time to hit your goals.",
                    "WORKOUT_REMINDER");
        }
    }

    /** If calorie intake > 120% of goal → alert */
    private void checkCalorieOverage(User user, LocalDate today) {
        int calorieGoal = userFitnessProfileRepository.findByUserId(user.getId())
                .map(p -> p.getDailyCalorieGoal()).orElse(0);
        if (calorieGoal <= 0) return;

        int consumed = caloriesLogRepository
                .findByUserIdAndDateOrderByCreatedAtAsc(user.getId(), today)
                .stream().mapToInt(c -> c.getConsumed()).sum();

        if (consumed > calorieGoal * 1.2) {
            notificationService.sendNotification(user,
                    "⚠️ You've exceeded your calorie goal by 20%+ today (" + consumed + " / " + calorieGoal + " kcal).",
                    "CALORIE_ALERT");
        }
    }
}
