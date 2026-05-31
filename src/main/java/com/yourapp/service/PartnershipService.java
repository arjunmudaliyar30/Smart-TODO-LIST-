package com.yourapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yourapp.config.OpenAIConfig;
import com.yourapp.model.AccountabilityScore;
import com.yourapp.model.Partnership;
import com.yourapp.model.User;
import com.yourapp.repository.AccountabilityScoreRepository;
import com.yourapp.repository.PartnershipRepository;
import com.yourapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Peer accountability partnerships.
 * Privacy: only accountability scores shared between partners (NOT full task/goal data).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"null","resource"})
public class PartnershipService {

    private final PartnershipRepository         partnershipRepository;
    private final UserRepository                userRepository;
    private final AccountabilityScoreRepository scoreRepository;
    private final WebPushService                webPushService;
    private final OpenAIConfig                  openAIConfig;
    private final OkHttpClient                  okHttpClient;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");

    /** Send an invite to another user (by email or ID) */
    public Partnership invite(String fromUserId, String toEmail) {
        User target = userRepository.findByEmail(toEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + toEmail));

        String toUserId = target.getId();
        if (fromUserId.equals(toUserId)) throw new IllegalArgumentException("Cannot invite yourself");

        // Check if partnership already exists
        Optional<Partnership> existing = partnershipRepository.findByUserPair(fromUserId, toUserId);
        if (existing.isPresent()) return existing.get();

        Partnership p = Partnership.builder()
                .userId1(fromUserId)
                .userId2(toUserId)
                .build();
        Partnership saved = partnershipRepository.save(p);

        try {
            User from = userRepository.findById(fromUserId).orElse(null);
            String fromName = from != null ? from.getFullName() : "Someone";
            webPushService.sendPush(toUserId, "Partnership Invite", fromName + " wants to be your accountability partner!");
        } catch (Exception e) {
            log.warn("PartnershipService invite push failed: {}", e.getMessage());
        }

        return saved;
    }

    public Partnership accept(String userId, String partnershipId) {
        Partnership p = partnershipRepository.findById(partnershipId)
                .orElseThrow(() -> new IllegalArgumentException("Partnership not found"));
        if (!p.getUserId2().equals(userId)) throw new IllegalArgumentException("Not authorized");
        p.setStatus("active");
        return partnershipRepository.save(p);
    }

    public Partnership decline(String userId, String partnershipId) {
        Partnership p = partnershipRepository.findById(partnershipId)
                .orElseThrow(() -> new IllegalArgumentException("Partnership not found"));
        if (!p.getUserId2().equals(userId)) throw new IllegalArgumentException("Not authorized");
        p.setStatus("declined");
        return partnershipRepository.save(p);
    }

    public List<Partnership> getMine(String userId) {
        return partnershipRepository.findByUserId(userId);
    }

    /**
     * Returns leaderboard: current user + all active partners,
     * showing only accountability score (not task/goal details).
     */
    public List<Map<String, Object>> getLeaderboard(String userId) {
        List<Partnership> partnerships = partnershipRepository.findByUserId(userId);
        Set<String> participantIds = new HashSet<>();
        participantIds.add(userId);
        partnerships.stream()
                .filter(p -> "active".equals(p.getStatus()))
                .forEach(p -> {
                    participantIds.add(p.getUserId1());
                    participantIds.add(p.getUserId2());
                });

        LocalDate today = LocalDate.now();
        return participantIds.stream()
                .map(uid -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    User user = userRepository.findById(uid).orElse(null);
                    entry.put("userId", uid);
                    entry.put("name", user != null ? user.getFullName() : "Unknown");
                    entry.put("isYou", uid.equals(userId));
                    Optional<AccountabilityScore> score = scoreRepository.findByUserIdAndDate(uid, today);
                    entry.put("score", score.map(AccountabilityScore::getScore).orElse(0));
                    return entry;
                })
                .sorted((a, b) -> Integer.compare((int) b.get("score"), (int) a.get("score")))
                .collect(Collectors.toList());
    }

    /** Send "cheering on" push to active partner */
    public void encourage(String fromUserId, String partnershipId) {
        Partnership p = partnershipRepository.findById(partnershipId)
                .orElseThrow(() -> new IllegalArgumentException("Partnership not found"));
        if (!p.getUserId1().equals(fromUserId) && !p.getUserId2().equals(fromUserId)) {
            throw new IllegalArgumentException("Not part of this partnership");
        }
        String toUserId = p.getUserId1().equals(fromUserId) ? p.getUserId2() : p.getUserId1();
        User from = userRepository.findById(fromUserId).orElse(null);
        String fromName = from != null ? from.getFullName() : "Your partner";
        webPushService.sendPush(toUserId, "You've got support!", fromName + " is cheering you on!");
    }

    /** Generates an AI weekly challenge for partners */
    public String getChallenge(String userId) {
        Optional<Partnership> activeOpt = partnershipRepository.findActiveByUserId(userId);
        if (activeOpt.isEmpty()) return "No active partnership. Invite a friend to get a challenge!";

        Partnership active = activeOpt.get();
        String partnerId = active.getUserId1().equals(userId) ? active.getUserId2() : active.getUserId1();

        Optional<AccountabilityScore> myScore = scoreRepository.findByUserIdAndDate(userId, LocalDate.now());
        Optional<AccountabilityScore> partnerScore = scoreRepository.findByUserIdAndDate(partnerId, LocalDate.now());

        int myS = myScore.map(AccountabilityScore::getScore).orElse(0);
        int parS = partnerScore.map(AccountabilityScore::getScore).orElse(0);

        String prompt = "Two accountability partners have scores today: User=" + myS + " Partner=" + parS
                + ". Generate a fun weekly challenge (max 30 words) that both can do to improve their score. "
                + "Be specific and motivating.";

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
                if (!resp.isSuccessful()) return "Challenge: Do 5 tasks and 1 workout this week!";
                JsonNode root = objectMapper.readTree(resp.body().string());
                return root.path("choices").path(0).path("message").path("content")
                        .asText("Challenge: Do 5 tasks and 1 workout this week!");
            }
        } catch (IOException e) {
            log.error("PartnershipService callGroq error: {}", e.getMessage());
            return "Challenge: Do 5 tasks and 1 workout this week!";
        }
    }
}
