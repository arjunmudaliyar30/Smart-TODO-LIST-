package com.yourapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yourapp.config.OpenAIConfig;
import com.yourapp.model.LifeArea;
import com.yourapp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"null","resource"})
public class LifeAreaService {

    private final LifeAreaRepository  lifeAreaRepository;
    private final TaskRepository      taskRepository;
    private final GoalRepository      goalRepository;
    private final OpenAIConfig        openAIConfig;
    private final OkHttpClient        okHttpClient;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");

    public LifeArea create(String userId, String name, String color) {
        LifeArea area = LifeArea.builder()
                .userId(userId)
                .name(name)
                .color(color)
                .build();
        return lifeAreaRepository.save(area);
    }

    public List<LifeArea> getAll(String userId) {
        return lifeAreaRepository.findByUserIdOrderByCreatedAtAsc(userId);
    }

    public void delete(String userId, String areaId) {
        lifeAreaRepository.findByIdAndUserId(areaId, userId)
                .ifPresent(lifeAreaRepository::delete);
    }

    /**
     * Checks which life areas have had no task or goal activity in 5+ days
     * and returns an AI warning about neglected areas.
     */
    public String getInsights(String userId) {
        List<LifeArea> areas = lifeAreaRepository.findByUserIdOrderByCreatedAtAsc(userId);
        if (areas.isEmpty()) return "No life areas defined yet. Add some to track balance!";

        LocalDate cutoff = LocalDate.now().minusDays(4); // 5+ days = before this date

        StringBuilder sb = new StringBuilder();
        for (LifeArea area : areas) {
            boolean hasRecentTask = taskRepository.findByUserIdOrderByCreatedAtDesc(userId)
                    .stream().anyMatch(t -> area.getId().equals(t.getLifeAreaId())
                            && t.getScheduledDate() != null
                            && !t.getScheduledDate().isBefore(cutoff));

            boolean hasRecentGoal = goalRepository.findByUserIdOrderByCreatedAtDesc(userId)
                    .stream().anyMatch(g -> area.getId().equals(g.getLifeAreaId()));

            if (!hasRecentTask && !hasRecentGoal) {
                sb.append("\"").append(area.getName()).append("\" — no recent activity. ");
            }
        }

        if (sb.isEmpty()) return "Great balance! All life areas have recent activity.";

        String neglected = sb.toString().trim();
        String prompt = "User's neglected life areas (no activity in 5+ days): " + neglected
                + " Write a warm, brief (max 30 words) motivational insight about work-life balance "
                + "and suggest prioritizing these areas.";

        return callGroq(prompt);
    }

    private String callGroq(String userPrompt) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", openAIConfig.getModel());
            ArrayNode messages = body.putArray("messages");
            ObjectNode msg = messages.addObject();
            msg.put("role", "user");
            msg.put("content", userPrompt);
            body.put("max_tokens", 120);

            Request req = new Request.Builder()
                    .url(openAIConfig.getApiUrl())
                    .addHeader("Authorization", "Bearer " + openAIConfig.getApiKey())
                    .post(RequestBody.create(objectMapper.writeValueAsString(body), JSON_MEDIA))
                    .build();

            try (Response resp = okHttpClient.newCall(req).execute()) {
                if (!resp.isSuccessful()) return "Consider giving attention to neglected life areas.";
                JsonNode root = objectMapper.readTree(resp.body().string());
                return root.path("choices").path(0).path("message").path("content")
                        .asText("Consider giving attention to neglected life areas.");
            }
        } catch (IOException e) {
            log.error("LifeAreaService callGroq error: {}", e.getMessage());
            return "Consider giving attention to neglected life areas.";
        }
    }
}
