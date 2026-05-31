package com.yourapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yourapp.config.OpenAIConfig;
import com.yourapp.model.Habit;
import com.yourapp.model.HabitLog;
import com.yourapp.repository.HabitLogRepository;
import com.yourapp.repository.HabitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"null","resource"})
public class HabitService {

    private final HabitRepository    habitRepository;
    private final HabitLogRepository habitLogRepository;
    private final OpenAIConfig       openAIConfig;
    private final OkHttpClient       okHttpClient;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");

    // ---- CRUD ----

    public Habit create(String userId, String name, List<String> targetDays, String lifeArea) {
        Habit habit = Habit.builder()
                .userId(userId)
                .name(name)
                .targetDays(targetDays != null ? targetDays : List.of())
                .lifeArea(lifeArea)
                .build();
        return habitRepository.save(habit);
    }

    public List<Habit> getActive(String userId) {
        return habitRepository.findByUserIdAndIsActiveTrue(userId);
    }

    public Habit update(String userId, String habitId, String name, List<String> targetDays,
                        String lifeArea, Boolean active) {
        Habit habit = habitRepository.findByIdAndUserId(habitId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Habit not found"));
        if (name      != null) habit.setName(name);
        if (targetDays != null) habit.setTargetDays(targetDays);
        if (lifeArea  != null) habit.setLifeArea(lifeArea);
        if (active    != null) habit.setActive(active);
        return habitRepository.save(habit);
    }

    public void delete(String userId, String habitId) {
        Habit habit = habitRepository.findByIdAndUserId(habitId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Habit not found"));
        habit.setActive(false);
        habitRepository.save(habit);
    }

    // ---- Logging ----

    public HabitLog log(String userId, String habitId, boolean completed) {
        LocalDate today = LocalDate.now();
        // Validate ownership
        habitRepository.findByIdAndUserId(habitId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Habit not found"));

        HabitLog entry = habitLogRepository.findByUserIdAndHabitIdAndDate(userId, habitId, today)
                .orElseGet(() -> HabitLog.builder()
                        .userId(userId)
                        .habitId(habitId)
                        .date(today)
                        .build());
        entry.setCompleted(completed);
        return habitLogRepository.save(entry);
    }

    public List<HabitLog> getTodayLogs(String userId) {
        return habitLogRepository.findByUserIdAndDate(userId, LocalDate.now());
    }

    public List<HabitLog> getWeeklyLogs(String userId) {
        LocalDate today = LocalDate.now();
        return habitLogRepository.findByUserIdAndDateBetween(userId, today.minusDays(6), today);
    }

    // ---- AI Insights ----

    public String getInsights(String userId) {
        List<Habit> habits = habitRepository.findByUserIdAndIsActiveTrue(userId);
        if (habits.isEmpty()) return "No active habits to analyze.";

        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(6);
        List<HabitLog> logs = habitLogRepository.findByUserIdAndDateBetween(userId, weekAgo, today);

        StringBuilder context = new StringBuilder();
        context.append("User has these active habits:\n");
        for (Habit h : habits) {
            long done = logs.stream()
                    .filter(l -> l.getHabitId().equals(h.getId()) && l.isCompleted())
                    .count();
            context.append("- \"").append(h.getName()).append("\" target days: ")
                   .append(h.getTargetDays()).append(", completed ").append(done).append("/7 days\n");
        }
        context.append("\nAnalyze skip patterns and provide 2-3 actionable insights for habit consistency.");

        return callGroq(context.toString());
    }

    private String callGroq(String userPrompt) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", openAIConfig.getModel());
            ArrayNode messages = body.putArray("messages");
            ObjectNode msg = messages.addObject();
            msg.put("role", "user");
            msg.put("content", userPrompt);
            body.put("max_tokens", 400);

            Request req = new Request.Builder()
                    .url(openAIConfig.getApiUrl())
                    .addHeader("Authorization", "Bearer " + openAIConfig.getApiKey())
                    .post(RequestBody.create(objectMapper.writeValueAsString(body), JSON_MEDIA))
                    .build();

            try (Response resp = okHttpClient.newCall(req).execute()) {
                if (!resp.isSuccessful()) return "Insights unavailable.";
                JsonNode root = objectMapper.readTree(resp.body().string());
                return root.path("choices").path(0).path("message").path("content").asText("Insights unavailable.");
            }
        } catch (IOException e) {
            log.error("HabitService callGroq error: {}", e.getMessage());
            return "Insights unavailable.";
        }
    }
}
