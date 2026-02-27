package com.yourapp.controller;

import com.yourapp.dto.ApiResponse;
import com.yourapp.dto.GoalRequest;
import com.yourapp.model.Goal;
import com.yourapp.model.Goal.GoalCategory;
import com.yourapp.model.Goal.GoalStatus;
import com.yourapp.model.User;
import com.yourapp.service.AiService;
import com.yourapp.service.GoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;
    private final AiService aiService;

    @PostMapping
    public ResponseEntity<ApiResponse<Goal>> createGoal(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody GoalRequest request) {

        Goal goal = goalService.createGoal(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Goal created", goal));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Goal>>> getGoals(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) GoalStatus status,
            @RequestParam(required = false) GoalCategory category) {

        List<Goal> goals;
        if (status != null) {
            goals = goalService.getGoalsByStatus(user.getId(), status);
        } else if (category != null) {
            goals = goalService.getGoalsByCategory(user.getId(), category);
        } else {
            goals = goalService.getUserGoals(user.getId());
        }
        return ResponseEntity.ok(ApiResponse.success(goals));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Goal>> getGoal(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {

        return ResponseEntity.ok(ApiResponse.success(goalService.getGoalById(user.getId(), id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Goal>> updateGoal(
            @AuthenticationPrincipal User user,
            @PathVariable String id,
            @Valid @RequestBody GoalRequest request) {

        Goal updated = goalService.updateGoal(user.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Goal updated", updated));
    }

    @PostMapping("/{id}/analyze")
    public ResponseEntity<ApiResponse<Goal>> analyzeGoal(
            @AuthenticationPrincipal User user,
            @PathVariable String id) throws IOException {

        Goal goal = goalService.getGoalById(user.getId(), id);
        String insight = aiService.analyzeGoal(
                goal.getTitle(),
                goal.getDescription(),
                goal.getCategory() != null ? goal.getCategory().name() : "GENERAL");
        Goal updated = goalService.saveAiInsight(user.getId(), id, insight);
        return ResponseEntity.ok(ApiResponse.success("AI analysis complete", updated));
    }

    /** POST /api/goals/{id}/collaborators — add a collaborator by email (resolves email→MongoDB ID) */
    @PostMapping("/{id}/collaborators")
    public ResponseEntity<ApiResponse<com.yourapp.model.Goal>> addCollaborator(
            @AuthenticationPrincipal User user,
            @PathVariable String id,
            @RequestBody java.util.Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank())
            return ResponseEntity.badRequest().body(ApiResponse.error("Email is required"));
        com.yourapp.model.Goal updated =
                goalService.addCollaboratorByEmail(user.getId(), id, email.trim().toLowerCase());
        return ResponseEntity.ok(ApiResponse.success("Collaborator added", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteGoal(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {

        goalService.deleteGoal(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Goal deleted", null));
    }
}
