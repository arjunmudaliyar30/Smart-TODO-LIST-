package com.yourapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yourapp.config.OpenAIConfig;
import com.yourapp.dto.DailyNoteRequest;
import com.yourapp.model.EveningReflection;
import com.yourapp.repository.EveningReflectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles evening reflections.
 * On save: asks Groq for an AI summary, then persists to evening_reflections
 * and also saves to daily notes (title: "Reflection — [date]").
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"null","resource"})
public class EveningReflectionService {

    private final EveningReflectionRepository reflectionRepository;
    private final DailyNoteService            dailyNoteService;
    private final OpenAIConfig                openAIConfig;
    private final OkHttpClient                okHttpClient;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");

    /** Saves or updates today's reflection. Generates AI summary. */
    public EveningReflection save(String userId, String q1, String q2, String q3) {
        LocalDate today = LocalDate.now();

        String aiSummary = generateSummary(q1, q2, q3);

        EveningReflection reflection = reflectionRepository.findByUserIdAndDate(userId, today)
                .orElseGet(() -> EveningReflection.builder()
                        .userId(userId)
                        .date(today)
                        .build());
        reflection.setQ1Answer(q1);
        reflection.setQ2Answer(q2);
        reflection.setQ3Answer(q3);
        reflection.setAiSummary(aiSummary);
        EveningReflection saved = reflectionRepository.save(reflection);

        // Save to DailyNote for journaling
        try {
            DailyNoteRequest noteReq = new DailyNoteRequest();
            noteReq.setDate(today);
            noteReq.setContent("**Reflection — " + today + "**\n\n"
                    + "Q1: " + q1 + "\n\nQ2: " + q2 + "\n\nQ3: " + q3
                    + (aiSummary != null ? "\n\n*AI Summary: " + aiSummary + "*" : ""));
            dailyNoteService.createOrUpdateNote(userId, noteReq);
        } catch (Exception e) {
            log.warn("Evening reflection failed to save to daily notes: {}", e.getMessage());
        }

        return saved;
    }

    public List<EveningReflection> getHistory(String userId) {
        return reflectionRepository.findByUserIdOrderByDateDesc(userId);
    }

    /** Sends last 14 reflections to Groq for pattern analysis */
    public String getPatterns(String userId) {
        List<EveningReflection> recent = reflectionRepository.findByUserIdOrderByDateDesc(userId)
                .stream().limit(14).collect(Collectors.toList());

        if (recent.isEmpty()) return "No reflections yet. Start journaling to see patterns.";

        StringBuilder context = new StringBuilder();
        context.append("Analyze these ").append(recent.size()).append(" evening reflections and find patterns:\n\n");
        for (int i = 0; i < recent.size(); i++) {
            EveningReflection r = recent.get(i);
            context.append("[").append(r.getDate()).append("] ")
                   .append("Q1: ").append(r.getQ1Answer() != null ? r.getQ1Answer() : "N/A").append(" | ")
                   .append("Q2: ").append(r.getQ2Answer() != null ? r.getQ2Answer() : "N/A").append(" | ")
                   .append("Q3: ").append(r.getQ3Answer() != null ? r.getQ3Answer() : "N/A").append("\n");
        }
        context.append("\nIdentify: 1. Common wins  2. Recurring blockers  3. Mood trends  4. One suggestion.");

        return callGroq(context.toString(), 500);
    }

    private String generateSummary(String q1, String q2, String q3) {
        String prompt = "Evening reflection: "
                + "Highlight: " + q1 + " | "
                + "Challenge: " + q2 + " | "
                + "Tomorrow: " + q3
                + ". Write a single insightful sentence (max 25 words) summarizing today.";
        return callGroq(prompt, 80);
    }

    private String callGroq(String userPrompt, int maxTokens) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", openAIConfig.getModel());
            ArrayNode messages = body.putArray("messages");
            ObjectNode msg = messages.addObject();
            msg.put("role", "user");
            msg.put("content", userPrompt);
            body.put("max_tokens", maxTokens);

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
            log.error("EveningReflectionService callGroq error: {}", e.getMessage());
            return null;
        }
    }
}
