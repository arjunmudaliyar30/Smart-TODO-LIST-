package com.yourapp.service;

import com.yourapp.model.*;
import com.yourapp.model.Task.TaskStatus;
import com.yourapp.model.Goal.GoalStatus;
import com.yourapp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Central AI context builder.
 * Aggregates all user data into one rich String for Groq prompts.
 * Does NOT modify any existing service — read-only aggregation only.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class UserContextService {

    private final TaskRepository            taskRepository;
    private final GoalRepository            goalRepository;
    private final SessionRepository         sessionRepository;
    private final UserStreakRepository      streakRepository;
    private final DailyPerformanceRepository performanceRepository;
    private final FocusSessionRepository   focusSessionRepository;
    private final BrainChallengeRepository  brainChallengeRepository;
    private final DailyNoteRepository       dailyNoteRepository;
    private final MoodLogRepository         moodLogRepository;
    private final HabitRepository           habitRepository;
    private final HabitLogRepository        habitLogRepository;
    private final AccountabilityScoreRepository accountabilityScoreRepository;
    private final EveningReflectionRepository   eveningReflectionRepository;

    public String getContext(String userId) {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate weekAgo = today.minusDays(7);

        StringBuilder sb = new StringBuilder();
        sb.append("[USER CONTEXT — ").append(today.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))).append("]\n");

        // --- Tasks ---
        try {
            List<Task> allTasks = taskRepository.findByUserIdOrderByCreatedAtDesc(userId);
            List<Task> todayTasks = allTasks.stream()
                    .filter(t -> t.getScheduledDate() != null && t.getScheduledDate().equals(today))
                    .collect(Collectors.toList());
            long done = todayTasks.stream().filter(t ->
                    t.getStatus() == TaskStatus.COMPLETED || t.getStatus() == TaskStatus.DONE).count();
            long pending = todayTasks.stream().filter(t -> t.getStatus() == TaskStatus.PENDING).count();
            long inProg  = todayTasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count();
            sb.append("Tasks today: ").append(todayTasks.size())
              .append(" total (done=").append(done)
              .append(", pending=").append(pending)
              .append(", in-progress=").append(inProg).append(")\n");
        } catch (Exception e) {
            log.debug("UserContext tasks error: {}", e.getMessage());
            sb.append("Tasks today: unavailable\n");
        }

        // --- Streaks ---
        try {
            streakRepository.findByUserId(userId).ifPresent(s ->
                sb.append("Task streak: ").append(s.getTaskStreak())
                  .append(" days | Workout streak: ").append(s.getWorkoutStreak())
                  .append(" days | Note streak: ").append(s.getNoteStreak()).append(" days\n")
            );
        } catch (Exception e) {
            log.debug("UserContext streak error: {}", e.getMessage());
        }

        // --- Mood Log (today) ---
        try {
            moodLogRepository.findByUserIdAndDate(userId, today).ifPresentOrElse(
                ml -> sb.append("Mood today: energy=").append(ml.getEnergy())
                         .append(", mood=").append(ml.getMood())
                         .append(", focus=").append(ml.getFocus()).append("/5\n"),
                () -> {
                    // Fall back to DailyPerformance for yesterday
                    performanceRepository.findByUserIdAndDate(userId, yesterday).ifPresent(p ->
                        sb.append("Yesterday performance score: ").append(String.format("%.0f", p.getScore()))
                          .append(" (task=").append(p.getTaskCompletionRatio() != null
                                  ? String.format("%.0f%%", p.getTaskCompletionRatio() * 100) : "n/a")
                          .append(")\n")
                    );
                }
            );
        } catch (Exception e) {
            log.debug("UserContext mood error: {}", e.getMessage());
        }

        // --- Active Goals ---
        try {
            List<Goal> goals = goalRepository.findByUserIdAndStatus(userId, GoalStatus.ACTIVE);
            if (!goals.isEmpty()) {
                String goalSummary = goals.stream()
                        .limit(5)
                        .map(g -> "\"" + g.getTitle() + "\" (" + g.getProgressPercent() + "%)")
                        .collect(Collectors.joining(", "));
                sb.append("Active goals (").append(goals.size()).append("): ").append(goalSummary).append("\n");
            }
        } catch (Exception e) {
            log.debug("UserContext goals error: {}", e.getMessage());
        }

        // --- Accountability Score ---
        try {
            accountabilityScoreRepository.findByUserIdAndDate(userId, today).ifPresent(s ->
                sb.append("Accountability score today: ").append(s.getScore()).append("/100\n")
            );
        } catch (Exception e) {
            log.debug("UserContext score error: {}", e.getMessage());
        }

        // --- Habits (today) ---
        try {
            List<Habit> habits = habitRepository.findByUserIdAndIsActiveTrue(userId);
            if (!habits.isEmpty()) {
                List<HabitLog> todayLogs = habitLogRepository.findByUserIdAndDate(userId, today);
                long completed = todayLogs.stream().filter(HabitLog::isCompleted).count();
                sb.append("Habits today: ").append(completed).append("/").append(habits.size()).append(" completed\n");
            }
        } catch (Exception e) {
            log.debug("UserContext habits error: {}", e.getMessage());
        }

        // --- Last 3 Sessions ---
        try {
            List<Session> sessions = sessionRepository.findByUserIdOrderBySessionDateDescCreatedAtDesc(userId);
            if (!sessions.isEmpty()) {
                String sessionSummary = sessions.stream()
                        .limit(3)
                        .map(s -> s.getType() + " " + (s.getDurationMinutes() != null ? s.getDurationMinutes() + "min" : ""))
                        .collect(Collectors.joining(", "));
                sb.append("Last sessions: ").append(sessionSummary).append("\n");
            }
        } catch (Exception e) {
            log.debug("UserContext sessions error: {}", e.getMessage());
        }

        // --- Focus sessions today ---
        try {
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = today.atTime(23, 59, 59);
            List<FocusSession> todayFocus = focusSessionRepository.findByUserId(userId).stream()
                    .filter(f -> f.getCreatedAt() != null
                            && !f.getCreatedAt().isBefore(startOfDay)
                            && !f.getCreatedAt().isAfter(endOfDay))
                    .collect(Collectors.toList());
            long focusDone = todayFocus.stream().filter(FocusSession::isCompleted).count();
            if (!todayFocus.isEmpty()) {
                sb.append("Focus sessions today: ").append(focusDone).append(" completed / ")
                  .append(todayFocus.size()).append(" total\n");
            }
        } catch (Exception e) {
            log.debug("UserContext focus error: {}", e.getMessage());
        }

        // --- Brain Coach (last 7 days) ---
        try {
            List<BrainChallenge> challenges = brainChallengeRepository.findByUserIdOrderByCreatedAtDesc(userId);
            List<BrainChallenge> recent = challenges.stream()
                    .filter(c -> c.getCreatedAt() != null
                            && c.getCreatedAt().isAfter(weekAgo.atStartOfDay()))
                    .collect(Collectors.toList());
            if (!recent.isEmpty()) {
                long correct = recent.stream().filter(c -> Boolean.TRUE.equals(c.getCorrect())).count();
                sb.append("Brain challenges (7d): ").append(correct).append(" correct / ")
                  .append(recent.size()).append(" attempted\n");
            }
        } catch (Exception e) {
            log.debug("UserContext brain error: {}", e.getMessage());
        }

        // --- Last 3 Evening Reflections ---
        try {
            List<EveningReflection> reflections = eveningReflectionRepository
                    .findByUserIdOrderByDateDesc(userId).stream().limit(3).collect(Collectors.toList());
            if (!reflections.isEmpty()) {
                sb.append("Last reflections (").append(reflections.size()).append("):\n");
                reflections.forEach(r -> sb.append("  - [").append(r.getDate()).append("] ")
                        .append(r.getQ1Answer() != null ? r.getQ1Answer().substring(0, Math.min(60, r.getQ1Answer().length())) : "")
                        .append("...\n"));
            }
        } catch (Exception e) {
            log.debug("UserContext reflections error: {}", e.getMessage());
        }

        // --- Last 3 Daily Notes ---
        try {
            List<DailyNote> notes = dailyNoteRepository
                    .findByUserIdAndDeletedFalseAndDateBetweenOrderByDateDesc(userId, weekAgo, today)
                    .stream().limit(3).collect(Collectors.toList());
            if (!notes.isEmpty()) {
                sb.append("Recent notes: ").append(notes.size()).append(" in last 7 days\n");
            }
        } catch (Exception e) {
            log.debug("UserContext notes error: {}", e.getMessage());
        }

        return sb.toString();
    }
}
