package com.yourapp.controller;

import com.yourapp.dto.ApiResponse;
import com.yourapp.model.BrainChallenge;
import com.yourapp.model.DecisionLog;
import com.yourapp.model.User;
import com.yourapp.service.BrainCoachService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/brain-coach")
@RequiredArgsConstructor
public class BrainCoachController {

    private final BrainCoachService brainCoachService;

    // ---- Challenges ----

    @PostMapping("/challenges/generate")
    public ResponseEntity<ApiResponse<BrainChallenge>> generate(
            @AuthenticationPrincipal User user,
            @RequestBody(required = false) Map<String, String> body) {

        String category   = body != null ? body.get("category")   : null;
        String difficulty = body != null ? body.get("difficulty")  : null;
        return ResponseEntity.ok(ApiResponse.success(
                brainCoachService.generateChallenge(user.getId(), category, difficulty)));
    }

    @GetMapping("/challenges")
    public ResponseEntity<ApiResponse<List<BrainChallenge>>> listChallenges(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(brainCoachService.listChallenges(user.getId())));
    }

    @PostMapping("/challenges/{id}/answer")
    public ResponseEntity<ApiResponse<BrainChallenge>> answer(
            @AuthenticationPrincipal User user,
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(
                brainCoachService.submitAnswer(user.getId(), id, body.getOrDefault("answer", ""))));
    }

    @DeleteMapping("/challenges/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteChallenge(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {
        brainCoachService.deleteChallenge(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }

    // ---- Decision Log ----

    @PostMapping("/decisions")
    public ResponseEntity<ApiResponse<DecisionLog>> createDecision(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(
                brainCoachService.createDecisionLog(
                        user.getId(),
                        body.getOrDefault("decision", ""),
                        body.getOrDefault("context", ""))));
    }

    @GetMapping("/decisions")
    public ResponseEntity<ApiResponse<List<DecisionLog>>> listDecisions(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(brainCoachService.listDecisionLogs(user.getId())));
    }

    @PatchMapping("/decisions/{id}/outcome")
    public ResponseEntity<ApiResponse<DecisionLog>> addOutcome(
            @AuthenticationPrincipal User user,
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(
                brainCoachService.updateOutcome(user.getId(), id, body.getOrDefault("outcome", ""))));
    }

    @DeleteMapping("/decisions/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDecision(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {
        brainCoachService.deleteDecisionLog(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }
}
