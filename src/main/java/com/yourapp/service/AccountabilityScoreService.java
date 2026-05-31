package com.yourapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yourapp.config.OpenAIConfig;
import com.yourapp.model.AccountabilityScore;
import com.yourapp.model.HabitLog;
import com.yourapp.model.MoodLog;
import com.yourapp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

/**
 * Calculates daily accountability score (0–100) and asks Groq for a one-line comment.
 * Score breakdown (max 100):
 *   tasks completed today       → up to 30 pts
 *   streak maintained today     → up to 20 pts
 *   session logged today        → up to 15 pts
 *   habits completed today      → up to 20 pts
 *   mood/energy logged today    → 10 pts
 *   evening reflection done     → 5 pts
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"null","resource"})
public class AccountabilityScoreService {

    private final AccountabilityScoreRepository scoreRepository;
    private final TaskRepository                taskRepository;
    private final UserStreakRepository          streakRepository;
    private final SessionRepository             sessionRepository;
    private final HabitRepository               habitRepository;
    private final HabitLogRepository            habitLogRepository;
    private final MoodLogRepository             moodLogRepository;
    private final EveningReflectionRepository   reflectionRepository;
    private final OpenAIConfig                  openAIConfig;
    private final OkHttpClient                  okHttpClient;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");

    public AccountabilityScore calculateAndSave(String userId) {
        LocalDate today = LocalDate.now();
        Map<String, Integer> breakdown = new LinkedHashMap<>();
        int total = 0;

        // --- Tasks (30 pts) ---
        try {
            var tasks = taskRepository.findByUserIdAndScheduledDate(userId, today);
            if (!tasks.isEmpty()) {
                long done = tasks.stream().filter(t ->
                        t.getStatus() != null &&
                        (t.getStatus().name().equals("COMPLETED") || t.getStatus().name().equals("DONE"))
                ).count();
                int pts = (int) Math.round((done * 1.0 / tasks.size()) * 30);
                breakdown.put("tasks", pts);
                total += pts;
            } else {
                breakdown.put("tasks", 0);
            }
        } catch (Exception e) {
            log.debug("Score tasks error: {}", e.getMessage());
            breakdown.put("tasks", 0);
        }

        // --- Streak (20 pts) ---
        try {
            var streakOpt = streakRepository.findByUserId(userId);
            if (streakOpt.isPresent()) {
                int taskStreak = streakOpt.get().getTaskStreak();
                int pts = taskStreak >= 7 ? 20 : taskStreak >= 3 ? 15 : taskStreak >= 1 ? 10 : 0;
                breakdown.put("streak", pts);
                total += pts;
            } else {
                breakdown.put("streak", 0);
            }
        } catch (Exception e) {
            log.debug("Score streak error: {}", e.getMessage());
            breakdown.put("streak", 0);
        }

        // --- Session today (15 pts) ---
        try {
            boolean hasSession = sessionRepository
                    .findByUserIdOrderBySessionDateDescCreatedAtDesc(userId)
                    .stream().anyMatch(s -> today.equals(s.getSessionDate()));
            int pts = hasSession ? 15 : 0;
            breakdown.put("session", pts);
            total += pts;
        } catch (Exception e) {
            log.debug("Score session error: {}", e.getMessage());
            breakdown.put("session", 0);
        }

        // --- Habits (20 pts) ---
        try {
            var habits = habitRepository.findByUserIdAndIsActiveTrue(userId);
            if (!habits.isEmpty()) {
                List<HabitLog> logs = habitLogRepository.findByUserIdAndDate(userId, today);
                long completed = logs.stream().filter(HabitLog::isCompleted).count();
                int pts = (int) Math.round((completed * 1.0 / habits.size()) * 20);
                breakdown.put("habits", pts);
                total += pts;
            } else {
                breakdown.put("habits", 0);
            }
        } catch (Exception e) {
            log.debug("Score habits error: {}", e.getMessage());
            breakdown.put("habits", 0);
        }

        // --- Mood logged (10 pts) ---
        try {
            Optional<MoodLog> mood = moodLogRepository.findByUserIdAndDate(userId, today);
            int pts = mood.isPresent() ? 10 : 0;
            breakdown.put("moodLogged", pts);
            total += pts;
        } catch (Exception e) {
            log.debug("Score mood error: {}", e.getMessage());
            breakdown.put("moodLogged", 0);
        }

        // --- Evening Reflection (5 pts) ---
        try {
            boolean reflected = reflectionRepository.findByUserIdAndDate(userId, today).isPresent();
            int pts = reflected ? 5 : 0;
            breakdown.put("reflection", pts);
            total += pts;
        } catch (Exception e) {
            log.debug("Score reflection error: {}", e.getMessage());
            breakdown.put("reflection", 0);
        }

        // --- AI Comment ---
        String aiComment = getAiComment(total, breakdown);

        // Upsert
        AccountabilityScore score = scoreRepository.findByUserIdAndDate(userId, today)
                .orElseGet(() -> AccountabilityScore.builder()
                        .userId(userId)
                        .date(today)
                        .build());
        score.setScore(total);
        score.setBreakdown(breakdown);
        score.setAiComment(aiComment);
        return scoreRepository.save(score);
    }

    public AccountabilityScore getToday(String userId) {
        return scoreRepository.findByUserIdAndDate(userId, LocalDate.now())
                .orElseGet(() -> calculateAndSave(userId));
    }

    public List<AccountabilityScore> getWeekly(String userId) {
        LocalDate today = LocalDate.now();
        return scoreRepository.findByUserIdAndDateBetweenOrderByDateDesc(userId, today.minusDays(6), today);
    }

    private String getAiComment(int score, Map<String, Integer> breakdown) {
        try {
            String prompt = "User's accountability score today: " + score + "/100. "
                    + "Breakdown: tasks=" + breakdown.getOrDefault("tasks", 0)
                    + " habits=" + breakdown.getOrDefault("habits", 0)
                    + " session=" + breakdown.getOrDefault("session", 0)
                    + " streak=" + breakdown.getOrDefault("streak", 0)
                    + " mood=" + breakdown.getOrDefault("moodLogged", 0)
                    + " reflection=" + breakdown.getOrDefault("reflection", 0) + ". "
                    + "Write a single encouraging/motivating sentence (max 20 words) as a personal coach. "
                    + "Be direct and specific to the score level.";

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
                if (!resp.isSuccessful()) return null;
                JsonNode root = objectMapper.readTree(resp.body().string());
                return root.path("choices").path(0).path("message").path("content").asText(null);
            }
        } catch (IOException e) {
            log.error("AccountabilityScore AI comment error: {}", e.getMessage());
            return null;
        }
    }
}
