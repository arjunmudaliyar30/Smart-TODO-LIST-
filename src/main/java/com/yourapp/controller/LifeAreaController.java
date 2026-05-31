package com.yourapp.controller;

import com.yourapp.dto.ApiResponse;
import com.yourapp.model.User;
import com.yourapp.service.LifeAreaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/life-areas")
@RequiredArgsConstructor
public class LifeAreaController {

    private final LifeAreaService lifeAreaService;

    /** POST /api/life-areas */
    @PostMapping
    public ResponseEntity<ApiResponse<Object>> create(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {
        String name  = body.getOrDefault("name", "").trim();
        String color = body.getOrDefault("color", "#6366f1");
        if (name.isEmpty()) return ResponseEntity.badRequest().body(ApiResponse.error("name is required"));
        return ResponseEntity.ok(ApiResponse.success("Life area created",
                lifeAreaService.create(user.getId(), name, color)));
    }

    /** GET /api/life-areas */
    @GetMapping
    public ResponseEntity<ApiResponse<Object>> getAll(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(lifeAreaService.getAll(user.getId())));
    }

    /** DELETE /api/life-areas/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> delete(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {
        lifeAreaService.delete(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Life area deleted", null));
    }

    /** GET /api/life-areas/insights */
    @GetMapping("/insights")
    public ResponseEntity<ApiResponse<Object>> getInsights(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(lifeAreaService.getInsights(user.getId())));
    }
}
