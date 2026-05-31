package com.yourapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yourapp.config.OpenAIConfig;
import com.yourapp.model.*;
import com.yourapp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Smart nudge engine.
 * Called by NudgeScheduler (hourly) or on-demand.
 * Max 2 nudges per user per day.
 * Push is sent via WebPushService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"null","resource"})
public class NudgeService {

    // max nudges per user per day
    private static final int MAX_DAILY_NUDGES = 2;

    private final NudgeLogRepository   nudgeLogRepository;
    private final UserStreakRepository  streakRepository;
    private final SessionRepository    sessionRepository;
    private final MoodLogRepository    moodLogRepository;
    private final HabitRepository      habitRepository;
    private final HabitLogRepository   habitLogRepository;
    private final OneThingRepository   oneThingRepository;
    private final AccountabilityScoreRepository scoreRepository;
    private final WebPushService       webPushService;
    private final OpenAIConfig         openAIConfig;
    private final OkHttpClient         okHttpClient;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");

    public List<NudgeLog> getLogs(String userId) {
        return nudgeLogRepository.findByUserIdOrderBySentAtDesc(userId);
    }

    /**
     * Check nudge conditions for a single user.
     * Called by the scheduler for all users.
     */
    public void checkAndNudge(String userId) {
        LocalDate today = LocalDate.now();
        List<NudgeLog> todayNudges = nudgeLogRepository.findByUserIdAndDate(userId, today);
        if (todayNudges.size() >= MAX_DAILY_NUDGES) return;

        // Check conditions in priority order
        String trigger = null;
        String situationSummary = null;

        // 1. Streak at risk (streak >= 2 and no qualifying activity yet today)
        try {
            var streakOpt = streakRepository.findByUserId(userId);
            if (streakOpt.isPresent()) {
                var streak = streakOpt.get();
                if (streak.getTaskStreak() >= 2 && streak.getLastTaskDate() != null
                        && !streak.getLastTaskDate().equals(today)) {
                    trigger = "STREAK_RISK";
                    situationSummary = "User has a " + streak.getTaskStreak() + "-day task streak at risk today.";
                }
            }
        } catch (Exception e) { log.debug("Nudge streak check: {}", e.getMessage()); }

        // 2. Habit skipped 3+ days
        if (trigger == null) {
            try {
                var habits = habitRepository.findByUserIdAndIsActiveTrue(userId);
                for (Habit h : habits) {
                    LocalDate threeDaysAgo = today.minusDays(3);
                    var logs = habitLogRepository.findByUserIdAndHabitIdAndDateBetween(
                            userId, h.getId(), threeDaysAgo, today.minusDays(1));
                    boolean skippedThreeDays = logs.stream().noneMatch(HabitLog::isCompleted);
                    if (logs.size() >= 3 && skippedThreeDays) {
                        trigger = "HABIT_SKIP";
                        situationSummary = "User has skipped habit \"" + h.getName() + "\" for 3+ days.";
                        break;
                    }
                }
            } catch (Exception e) { log.debug("Nudge habit check: {}", e.getMessage()); }
        }

        // 3. No session logged in 3 days
        if (trigger == null) {
            try {
                var sessions = sessionRepository.findByUserIdOrderBySessionDateDescCreatedAtDesc(userId);
                if (!sessions.isEmpty()) {
                    LocalDate lastSession = sessions.get(0).getSessionDate();
                    if (lastSession != null && lastSession.isBefore(today.minusDays(2))) {
                        trigger = "NO_SESSION";
                        situationSummary = "User has not logged a workout session in 3+ days.";
                    }
                }
            } catch (Exception e) { log.debug("Nudge session check: {}", e.getMessage()); }
        }

        // 4. Mood < 2 for 3 consecutive days
        if (trigger == null) {
            try {
                var logs = moodLogRepository.findByUserIdAndDateBetweenOrderByDateDesc(
                        userId, today.minusDays(3), today.minusDays(1));
                if (logs.size() >= 3 && logs.stream().allMatch(ml -> ml.getMood() < 2)) {
                    trigger = "LOW_MOOD";
                    situationSummary = "User's mood has been below 2/5 for the past 3 days.";
                }
            } catch (Exception e) { log.debug("Nudge mood check: {}", e.getMessage()); }
        }

        // 5. High score (>= 80) — positive reinforcement
        if (trigger == null) {
            try {
                var scoreOpt = scoreRepository.findByUserIdAndDate(userId, today);
                if (scoreOpt.isPresent() && scoreOpt.get().getScore() >= 80) {
                    trigger = "HIGH_SCORE";
                    situationSummary = "User achieved an accountability score of " + scoreOpt.get().getScore() + " today.";
                }
            } catch (Exception e) { log.debug("Nudge score check: {}", e.getMessage()); }
        }

        // 6. One Thing not started by 4PM
        if (trigger == null && LocalTime.now().getHour() >= 16) {
            try {
                var oneThing = oneThingRepository.findByUserIdAndDate(userId, today);
                if (oneThing.isPresent() && !oneThing.get().isCompleted()) {
                    trigger = "ONE_THING_IDLE";
                    situationSummary = "User's 'One Thing' for today is still incomplete at 4PM: \""
                            + oneThing.get().getTaskText() + "\".";
                }
            } catch (Exception e) { log.debug("Nudge one-thing check: {}", e.getMessage()); }
        }

        if (trigger == null) return; // No nudge needed

        String nudgeText = generateNudgeText(trigger, situationSummary);
        if (nudgeText == null || nudgeText.isBlank()) return;

        // Save nudge log
        NudgeLog nudgeLog = NudgeLog.builder()
                .userId(userId)
                .date(today)
                .nudgeText(nudgeText)
                .trigger(trigger)
                .build();
        nudgeLogRepository.save(nudgeLog);

        // Send push
        try {
            webPushService.sendPush(userId, "FORGE Nudge", nudgeText);
        } catch (Exception e) {
            log.warn("NudgeService push failed for userId={}: {}", userId, e.getMessage());
        }
    }

    private String generateNudgeText(String trigger, String situation) {
        try {
            String prompt = "Situation: " + situation
                    + " Write a single short nudge/motivational message (max 20 words) "
                    + "that would help the user take action. Be warm, direct, specific.";

            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", openAIConfig.getModel());
            ArrayNode messages = body.putArray("messages");
            ObjectNode msg = messages.addObject();
            msg.put("role", "user");
            msg.put("content", prompt);
            body.put("max_tokens", 60);

            Request req = new Request.Builder()
                    .url(openAIConfig.getApiUrl())
                    .addHeader("Authorization", "Bearer " + openAIConfig.getApiKey())
                    .post(RequestBody.create(objectMapper.writeValueAsString(body), JSON_MEDIA))
                    .build();

            try (Response resp = okHttpClient.newCall(req).execute()) {
                if (!resp.isSuccessful()) return fallbackNudge(trigger);
                JsonNode root = objectMapper.readTree(resp.body().string());
                return root.path("choices").path(0).path("message").path("content")
                        .asText(fallbackNudge(trigger));
            }
        } catch (IOException e) {
            log.error("NudgeService callGroq error: {}", e.getMessage());
            return fallbackNudge(trigger);
        }
    }

    private String fallbackNudge(String trigger) {
        return switch (trigger) {
            case "STREAK_RISK"    -> "Don't break your streak — log a task today!";
            case "HABIT_SKIP"     -> "Small habits compound. Log your habit now!";
            case "NO_SESSION"     -> "Your body misses you. Schedule a workout today!";
            case "LOW_MOOD"       -> "Take care of yourself. Even a 10-minute walk helps.";
            case "HIGH_SCORE"     -> "Amazing accountability score today! Keep it up!";
            case "ONE_THING_IDLE" -> "Your One Thing is still waiting. Tackle it before end of day!";
            default               -> "Stay on track — small actions lead to big results!";
        };
    }
}
