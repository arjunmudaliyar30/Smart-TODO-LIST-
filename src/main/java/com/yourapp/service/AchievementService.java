package com.yourapp.service;

import com.yourapp.dto.AchievementDTO;
import com.yourapp.model.Achievement;
import com.yourapp.model.Achievement.CriteriaType;
import com.yourapp.model.UserAchievement;
import com.yourapp.model.UserStreak;
import com.yourapp.repository.AchievementRepository;
import com.yourapp.repository.FocusSessionRepository;
import com.yourapp.repository.TaskRepository;
import com.yourapp.repository.UserAchievementRepository;
import com.yourapp.repository.WorkoutRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Phase 10: Gamification/Achievements
 *
 * Seeds badge definitions on startup.
 * Evaluates and unlocks achievements lazily (called after streak/score updates).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class AchievementService {

    private final AchievementRepository     achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final TaskRepository            taskRepository;
    private final WorkoutRepository         workoutRepository;
    private final FocusSessionRepository    focusSessionRepository;

    /** Seed achievements once on startup if they don't exist. */
    @PostConstruct
    public void seedAchievements() {
        seedIfAbsent("First Task Done",         "Complete your first task",              "✅", CriteriaType.TASK_COUNT,     1);
        seedIfAbsent("10 Tasks Crushed",        "Complete 10 tasks",                     "🎯", CriteriaType.TASK_COUNT,    10);
        seedIfAbsent("Century Achiever",        "Complete 100 tasks",                    "💯", CriteriaType.TASK_COUNT,   100);
        seedIfAbsent("First Workout",           "Complete your first workout",           "💪", CriteriaType.WORKOUT_COUNT,  1);
        seedIfAbsent("30 Workouts",             "Complete 30 workouts",                  "🏋️", CriteriaType.WORKOUT_COUNT, 30);
        seedIfAbsent("7-Day Task Streak",       "7 consecutive days completing tasks",   "🔥", CriteriaType.TASK_STREAK,    7);
        seedIfAbsent("30-Day Task Streak",      "30 consecutive days",                   "🌟", CriteriaType.TASK_STREAK,   30);
        seedIfAbsent("7-Day Workout Streak",    "7 consecutive days with a workout",     "⚡", CriteriaType.WORKOUT_STREAK, 7);
        seedIfAbsent("14-Day Calorie Streak",   "14 days within calorie goal",           "🥗", CriteriaType.CALORIE_STREAK, 14);
        seedIfAbsent("7-Day Note Streak",       "7 consecutive days writing notes",      "📝", CriteriaType.NOTE_STREAK,    7);
        seedIfAbsent("Focus Beginner",          "Log 60 total focus minutes",            "🎯", CriteriaType.FOCUS_MINUTES,  60);
        seedIfAbsent("Focus Master",            "Log 300 total focus minutes",           "🧠", CriteriaType.FOCUS_MINUTES, 300);
    }

    /** Called after events — evaluates all achievements for the user */
    public List<AchievementDTO> checkAndUnlockForUser(String userId, UserStreak streak) {
        List<Achievement> all = achievementRepository.findAll();
        List<AchievementDTO> unlocked = new ArrayList<>();

        for (Achievement a : all) {
            if (userAchievementRepository.existsByUserIdAndAchievementId(userId, a.getId())) continue;

            boolean earned = false;
            try {
                earned = evaluateCriteria(userId, a, streak);
            } catch (Exception e) {
                log.warn("Error evaluating achievement {} for user {}: {}", a.getName(), userId, e.getMessage());
            }

            if (earned) {
                UserAchievement ua = UserAchievement.builder()
                        .userId(userId)
                        .achievementId(a.getId())
                        .achievedDate(LocalDate.now())
                        .build();
                userAchievementRepository.save(ua);
                log.info("User {} unlocked achievement: {}", userId, a.getName());
                unlocked.add(toDTO(ua, a));
            }
        }
        return unlocked;
    }

    public List<AchievementDTO> getUserAchievements(String userId) {
        List<UserAchievement> uas = userAchievementRepository.findByUserId(userId);
        return uas.stream().map(ua ->
            achievementRepository.findById(ua.getAchievementId())
                .map(a -> toDTO(ua, a))
                .orElse(null)
        ).filter(a -> a != null).collect(Collectors.toList());
    }

    public long getAchievementCount(String userId) {
        return userAchievementRepository.countByUserId(userId);
    }

    // --- Private ---

    private boolean evaluateCriteria(String userId, Achievement a, UserStreak streak) {
        switch (a.getCriteriaType()) {
            case TASK_COUNT:
                return taskRepository.countByUserIdAndStatus(userId, com.yourapp.model.Task.TaskStatus.COMPLETED)
                     + taskRepository.countByUserIdAndStatus(userId, com.yourapp.model.Task.TaskStatus.DONE)
                     >= a.getCriteriaValue();

            case WORKOUT_COUNT:
                return workoutRepository.countByUserIdAndStatus(userId,
                        com.yourapp.model.Workout.WorkoutStatus.COMPLETED) >= a.getCriteriaValue();

            case TASK_STREAK:
                return streak != null && streak.getTaskStreak() >= a.getCriteriaValue();

            case WORKOUT_STREAK:
                return streak != null && streak.getWorkoutStreak() >= a.getCriteriaValue();

            case CALORIE_STREAK:
                return streak != null && streak.getCalorieStreak() >= a.getCriteriaValue();

            case NOTE_STREAK:
                return streak != null && streak.getNoteStreak() >= a.getCriteriaValue();

            case FOCUS_MINUTES:
                int totalFocusMin = focusSessionRepository.findByUserIdAndCompletedTrue(userId).stream()
                        .mapToInt(s -> s.getDurationMinutes())
                        .sum();
                return totalFocusMin >= a.getCriteriaValue();

            default:
                return false;
        }
    }

    private void seedIfAbsent(String name, String desc, String icon, CriteriaType type, int value) {
        if (achievementRepository.findByName(name).isEmpty()) {
            achievementRepository.save(Achievement.builder()
                    .name(name).description(desc).badgeIcon(icon)
                    .criteriaType(type).criteriaValue(value)
                    .build());
            log.info("Seeded achievement: {}", name);
        }
    }

    private AchievementDTO toDTO(UserAchievement ua, Achievement a) {
        return AchievementDTO.builder()
                .id(ua.getId())
                .achievementId(a.getId())
                .name(a.getName())
                .description(a.getDescription())
                .badgeIcon(a.getBadgeIcon())
                .achievedDate(ua.getAchievedDate())
                .build();
    }
}
