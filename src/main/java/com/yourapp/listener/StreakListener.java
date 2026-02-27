package com.yourapp.listener;

import com.yourapp.event.*;
import com.yourapp.model.UserStreak;
import com.yourapp.repository.UserStreakRepository;
import com.yourapp.service.AchievementService;
import com.yourapp.service.PerformanceScoreService;
import com.yourapp.service.StreakService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Phase 9: Event Listener — Streak updates + Achievement checks.
 *
 * All methods are @Async so they don't block the main request thread.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StreakListener {

    private final StreakService           streakService;
    private final PerformanceScoreService performanceScoreService;
    private final AchievementService      achievementService;
    private final UserStreakRepository    userStreakRepository;

    @Async
    @EventListener
    public void onTaskCompleted(TaskCompletedEvent event) {
        try {
            streakService.recordTaskCompleted(event.getUserId(), event.getCompletedDate());
            performanceScoreService.calculateAndStore(event.getUserId(), event.getCompletedDate());
            checkAchievements(event.getUserId());
        } catch (Exception e) {
            log.error("StreakListener.onTaskCompleted error: {}", e.getMessage(), e);
        }
    }

    @Async
    @EventListener
    public void onWorkoutCompleted(WorkoutCompletedEvent event) {
        try {
            streakService.recordWorkoutCompleted(event.getUserId(), event.getCompletedDate());
            performanceScoreService.calculateAndStore(event.getUserId(), event.getCompletedDate());
            checkAchievements(event.getUserId());
        } catch (Exception e) {
            log.error("StreakListener.onWorkoutCompleted error: {}", e.getMessage(), e);
        }
    }

    @Async
    @EventListener
    public void onNoteWritten(NoteWrittenEvent event) {
        try {
            streakService.recordNoteWritten(event.getUserId(), event.getNoteDate());
            performanceScoreService.calculateAndStore(event.getUserId(), event.getNoteDate());
            checkAchievements(event.getUserId());
        } catch (Exception e) {
            log.error("StreakListener.onNoteWritten error: {}", e.getMessage(), e);
        }
    }

    @Async
    @EventListener
    public void onCaloriesLogged(CaloriesLoggedEvent event) {
        try {
            boolean withinGoal = event.getCalorieGoal() > 0
                    && event.getTotalConsumed() <= event.getCalorieGoal() * 1.1;
            streakService.recordCaloriesLogged(event.getUserId(), event.getLogDate(), withinGoal);
            performanceScoreService.calculateAndStore(event.getUserId(), event.getLogDate());
            checkAchievements(event.getUserId());
        } catch (Exception e) {
            log.error("StreakListener.onCaloriesLogged error: {}", e.getMessage(), e);
        }
    }

    @Async
    @EventListener
    public void onFocusSessionCompleted(FocusSessionCompletedEvent event) {
        try {
            performanceScoreService.calculateAndStore(event.getUserId(), event.getSessionDate());
            checkAchievements(event.getUserId());
        } catch (Exception e) {
            log.error("StreakListener.onFocusSessionCompleted error: {}", e.getMessage(), e);
        }
    }

    private void checkAchievements(String userId) {
        UserStreak streak = userStreakRepository.findByUserId(userId).orElse(null);
        achievementService.checkAndUnlockForUser(userId, streak);
    }
}
