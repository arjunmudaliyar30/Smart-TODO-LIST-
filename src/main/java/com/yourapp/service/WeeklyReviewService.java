package com.yourapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yourapp.config.OpenAIConfig;
import com.yourapp.dto.DailyNoteRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Generates AI weekly review on Sunday 8PM IST.
 * Triggered by WeeklyReviewScheduler.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"null","resource"})
public class WeeklyReviewService {

    private final UserContextService userContextService;
    private final DailyNoteService   dailyNoteService;
    private final WebPushService     webPushService;
    private final MilestoneService   milestoneService;
    private final OpenAIConfig       openAIConfig;
    private final OkHttpClient       okHttpClient;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");

    /** Called by scheduler for one user */
    public void generateWeeklyReview(String userId) {
        try {
            String context = userContextService.getContext(userId);
            String prompt = "You are a personal coach writing a weekly review for a user.\n"
                    + context
                    + "\n\nWrite a 5-part weekly review:\n"
                    + "1. WINS (what went well this week)\n"
                    + "2. CHALLENGES (what was difficult)\n"
                    + "3. PATTERNS (what trends do you notice)\n"
                    + "4. INSIGHTS (what to improve next week)\n"
                    + "5. NEXT WEEK FOCUS (one concrete goal)\n"
                    + "Keep each part to 2-3 sentences. Be specific and encouraging.";

            String content = callGroq(prompt);

            LocalDate today = LocalDate.now();
            String weekLabel = "Week of " + today.minusDays(6).format(DateTimeFormatter.ofPattern("MMM dd"));

            DailyNoteRequest noteReq = new DailyNoteRequest();
            noteReq.setDate(today);
            noteReq.setContent("# Weekly Review — " + weekLabel + "\n\n" + content);
            dailyNoteService.createOrUpdateNote(userId, noteReq);

            webPushService.sendPush(userId, "📊 Your Weekly Review is Ready", "Check your FORGE weekly review for " + weekLabel);

            // Award milestone for first weekly review
            milestoneService.checkAndAward(userId, MilestoneService.FIRST_WEEKLY_REVIEW, "First Weekly Review completed!");

        } catch (Exception e) {
            log.error("WeeklyReviewService error for userId={}: {}", userId, e.getMessage());
        }
    }

    private String callGroq(String userPrompt) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", openAIConfig.getModel());
            ArrayNode messages = body.putArray("messages");
            ObjectNode msg = messages.addObject();
            msg.put("role", "user");
            msg.put("content", userPrompt);
            body.put("max_tokens", 600);

            Request req = new Request.Builder()
                    .url(openAIConfig.getApiUrl())
                    .addHeader("Authorization", "Bearer " + openAIConfig.getApiKey())
                    .post(RequestBody.create(objectMapper.writeValueAsString(body), JSON_MEDIA))
                    .build();

            try (Response resp = okHttpClient.newCall(req).execute()) {
                if (!resp.isSuccessful()) return "Weekly review unavailable. Keep pushing forward!";
                JsonNode root = objectMapper.readTree(resp.body().string());
                return root.path("choices").path(0).path("message").path("content")
                        .asText("Weekly review unavailable. Keep pushing forward!");
            }
        } catch (IOException e) {
            log.error("WeeklyReviewService callGroq error: {}", e.getMessage());
            return "Weekly review unavailable. Keep pushing forward!";
        }
    }
}
