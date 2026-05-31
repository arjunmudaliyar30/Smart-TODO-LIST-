package com.yourapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yourapp.config.OpenAIConfig;
import com.yourapp.model.MorningBriefing;
import com.yourapp.repository.MorningBriefingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * Generates (or retrieves) a personalized morning briefing via Groq.
 * Idempotent: generates once per user per day.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"null","resource"})
public class MorningBriefingService {

    private final MorningBriefingRepository briefingRepository;
    private final UserContextService         userContextService;
    private final OpenAIConfig               openAIConfig;
    private final OkHttpClient               okHttpClient;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");

    /** Returns today's briefing, generating it via Groq if it doesn't exist yet. */
    public MorningBriefing getTodayBriefing(String userId) {
        LocalDate today = LocalDate.now();
        return briefingRepository.findByUserIdAndDate(userId, today)
                .orElseGet(() -> generateAndSave(userId, today));
    }

    public List<MorningBriefing> getHistory(String userId) {
        return briefingRepository.findByUserIdOrderByDateDesc(userId);
    }

    private MorningBriefing generateAndSave(String userId, LocalDate date) {
        String context = userContextService.getContext(userId);
        String prompt = "You are a personal productivity coach delivering a morning briefing.\n"
                + context
                + "\nWrite a motivating morning briefing with:\n"
                + "1. One key priority for today (based on pending tasks/goals)\n"
                + "2. One insight about recent patterns\n"
                + "3. One motivating sentence\n"
                + "Keep it under 150 words. Use short paragraphs. Be specific, not generic.";

        String content = callGroq(prompt);

        MorningBriefing briefing = MorningBriefing.builder()
                .userId(userId)
                .date(date)
                .content(content)
                .build();
        return briefingRepository.save(briefing);
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
                if (!resp.isSuccessful()) return "Good morning! Focus on your top priority today and make it count.";
                JsonNode root = objectMapper.readTree(resp.body().string());
                return root.path("choices").path(0).path("message").path("content")
                        .asText("Good morning! Focus on your top priority today.");
            }
        } catch (IOException e) {
            log.error("MorningBriefingService callGroq error: {}", e.getMessage());
            return "Good morning! Start strong and focus on what matters most.";
        }
    }
}
