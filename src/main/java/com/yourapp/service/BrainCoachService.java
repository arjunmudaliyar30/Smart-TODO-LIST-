package com.yourapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yourapp.config.OpenAIConfig;
import com.yourapp.model.BrainChallenge;
import com.yourapp.model.DecisionLog;
import com.yourapp.repository.BrainChallengeRepository;
import com.yourapp.repository.DecisionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"null", "resource"})
public class BrainCoachService {

    private final OpenAIConfig openAIConfig;
    private final OkHttpClient okHttpClient;
    private final BrainChallengeRepository challengeRepo;
    private final DecisionLogRepository decisionLogRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");

    // -------------------------------------------------------------------------
    // CHALLENGES
    // -------------------------------------------------------------------------

    public BrainChallenge generateChallenge(String userId, String category, String difficulty) {
        String cat  = (category   != null && !category.isBlank())   ? category   : "Logic";
        String diff = (difficulty != null && !difficulty.isBlank()) ? difficulty : "Medium";

        String prompt = String.format(
            "Generate a single cognitive challenge for a productivity app. " +
            "Category: %s, Difficulty: %s. " +
            "Return ONLY valid JSON with keys: question (string), hint (string), answer (string). " +
            "No markdown, no explanation.", cat, diff);

        String raw = callGroq(prompt);
        try {
            JsonNode node = objectMapper.readTree(raw);
            BrainChallenge ch = BrainChallenge.builder()
                    .userId(userId)
                    .question(node.path("question").asText())
                    .hint(node.path("hint").asText())
                    .answer(node.path("answer").asText())
                    .category(cat)
                    .difficulty(diff)
                    .build();
            return challengeRepo.save(ch);
        } catch (Exception e) {
            log.warn("BrainCoach: could not parse challenge JSON: {}", raw);
            BrainChallenge fallback = BrainChallenge.builder()
                    .userId(userId)
                    .question("If you rearrange the letters \"CIFAIPC\", you get the name of a what?")
                    .hint("Think geography.")
                    .answer("PACIFIC (Ocean)")
                    .category(cat)
                    .difficulty(diff)
                    .build();
            return challengeRepo.save(fallback);
        }
    }

    public List<BrainChallenge> listChallenges(String userId) {
        return challengeRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public BrainChallenge submitAnswer(String userId, String challengeId, String userAnswer) {
        return challengeRepo.findById(challengeId)
                .filter(c -> c.getUserId().equals(userId))
                .map(c -> {
                    c.setUserAnswer(userAnswer);
                    boolean correct = c.getAnswer() != null &&
                            c.getAnswer().toLowerCase().contains(userAnswer.toLowerCase().trim());
                    c.setCorrect(correct);
                    c.setAnsweredAt(LocalDateTime.now());
                    return challengeRepo.save(c);
                })
                .orElseThrow(() -> new IllegalArgumentException("Challenge not found"));
    }

    public void deleteChallenge(String userId, String id) {
        challengeRepo.findById(id)
                .filter(c -> c.getUserId().equals(userId))
                .ifPresent(challengeRepo::delete);
    }

    // -------------------------------------------------------------------------
    // DECISION LOG
    // -------------------------------------------------------------------------

    public DecisionLog createDecisionLog(String userId, String decision, String context) {
        // Ask AI for an initial reflection
        String prompt = String.format(
            "A user made this decision: \"%s\". Context: \"%s\". " +
            "Give a concise, thoughtful 2-3 sentence reflection on this decision (risks, opportunities, considerations). " +
            "Be encouraging but honest. Plain text only, no markdown.", decision, context);

        String reflection = callGroqText(prompt);

        DecisionLog log = DecisionLog.builder()
                .userId(userId)
                .decision(decision)
                .context(context)
                .aiReflection(reflection)
                .build();
        return decisionLogRepo.save(log);
    }

    public List<DecisionLog> listDecisionLogs(String userId) {
        return decisionLogRepo.findByUserIdOrderByDecisionDateDescCreatedAtDesc(userId);
    }

    public DecisionLog updateOutcome(String userId, String id, String outcome) {
        return decisionLogRepo.findById(id)
                .filter(d -> d.getUserId().equals(userId))
                .map(d -> {
                    d.setOutcome(outcome);
                    // Update AI reflection to include outcome
                    String prompt = String.format(
                        "Decision: \"%s\". Original context: \"%s\". Actual outcome: \"%s\". " +
                        "Give a 2-3 sentence reflection on what was learned. Plain text, no markdown.",
                        d.getDecision(), d.getContext(), outcome);
                    d.setAiReflection(callGroqText(prompt));
                    return decisionLogRepo.save(d);
                })
                .orElseThrow(() -> new IllegalArgumentException("Decision log not found"));
    }

    public void deleteDecisionLog(String userId, String id) {
        decisionLogRepo.findById(id)
                .filter(d -> d.getUserId().equals(userId))
                .ifPresent(decisionLogRepo::delete);
    }

    // -------------------------------------------------------------------------
    // PRIVATE HELPERS
    // -------------------------------------------------------------------------

    /** Call Groq and return the raw first-choice message content. */
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
                if (!resp.isSuccessful()) return "{}";
                JsonNode root = objectMapper.readTree(resp.body().string());
                return root.path("choices").path(0).path("message").path("content").asText("{}");
            }
        } catch (IOException e) {
            log.error("BrainCoach callGroq error: {}", e.getMessage());
            return "{}";
        }
    }

    private String callGroqText(String userPrompt) {
        String raw = callGroq(userPrompt);
        // Remove any JSON wrapping if AI accidentally returned JSON
        if (raw.startsWith("{") || raw.startsWith("[")) {
            try {
                JsonNode n = objectMapper.readTree(raw);
                if (n.has("reflection")) return n.path("reflection").asText(raw);
            } catch (Exception ignored) { /* use raw */ }
        }
        return raw;
    }
}
