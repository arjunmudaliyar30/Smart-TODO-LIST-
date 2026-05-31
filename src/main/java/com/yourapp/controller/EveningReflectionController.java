package com.yourapp.controller;

import com.yourapp.dto.ApiResponse;
import com.yourapp.model.User;
import com.yourapp.service.EveningReflectionService;
import com.yourapp.service.MilestoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reflection")
@RequiredArgsConstructor
public class EveningReflectionController {

    private final EveningReflectionService reflectionService;
    private final MilestoneService         milestoneService;

    /**
     * POST /api/reflection
     * Body: { q1: "highlight", q2: "challenge", q3: "tomorrow" }
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Object>> save(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {
        String q1 = body.getOrDefault("q1", "");
        String q2 = body.getOrDefault("q2", "");
        String q3 = body.getOrDefault("q3", "");
        var saved = reflectionService.save(user.getId(), q1, q2, q3);

        // Award first-reflection milestone
        milestoneService.checkAndAward(user.getId(), MilestoneService.FIRST_REFLECTION,
                "Completed your first evening reflection!");

        return ResponseEntity.ok(ApiResponse.success("Reflection saved", saved));
    }

    /** GET /api/reflection/history */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Object>> getHistory(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(reflectionService.getHistory(user.getId())));
    }

    /** GET /api/reflection/patterns */
    @GetMapping("/patterns")
    public ResponseEntity<ApiResponse<Object>> getPatterns(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(reflectionService.getPatterns(user.getId())));
    }
}
