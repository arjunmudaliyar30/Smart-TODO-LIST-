package com.yourapp.controller;

import com.yourapp.dto.ApiResponse;
import com.yourapp.model.User;
import com.yourapp.service.OneThingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/one-thing")
@RequiredArgsConstructor
public class OneThingController {

    private final OneThingService oneThingService;

    /** POST /api/one-thing — set today's one thing */
    @PostMapping
    public ResponseEntity<ApiResponse<Object>> set(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {
        String taskText = body.getOrDefault("taskText", "").trim();
        if (taskText.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("taskText is required"));
        }
        return ResponseEntity.ok(ApiResponse.success("One Thing set",
                oneThingService.setTodayOneThing(user.getId(), taskText)));
    }

    /** GET /api/one-thing/today */
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<Object>> getToday(@AuthenticationPrincipal User user) {
        return oneThingService.getTodayOneThing(user.getId())
                .<ResponseEntity<ApiResponse<Object>>>map(ot -> ResponseEntity.ok(ApiResponse.success(ot)))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.success(null)));
    }

    /** PATCH /api/one-thing/today/complete */
    @PatchMapping("/today/complete")
    public ResponseEntity<ApiResponse<Object>> complete(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("One Thing completed!",
                oneThingService.completeToday(user.getId())));
    }
}
