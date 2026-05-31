package com.yourapp.controller;

import com.yourapp.dto.ApiResponse;
import com.yourapp.model.User;
import com.yourapp.service.HabitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/habits")
@RequiredArgsConstructor
public class HabitController {

    private final HabitService habitService;

    /** POST /api/habits — create habit */
    @PostMapping
    public ResponseEntity<ApiResponse<Object>> create(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        String name      = (String) body.get("name");
        @SuppressWarnings("unchecked")
        List<String> days = (List<String>) body.get("targetDays");
        String lifeArea  = (String) body.get("lifeArea");
        return ResponseEntity.ok(ApiResponse.success("Habit created",
                habitService.create(user.getId(), name, days, lifeArea)));
    }

    /** GET /api/habits */
    @GetMapping
    public ResponseEntity<ApiResponse<Object>> getAll(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(habitService.getActive(user.getId())));
    }

    /** PATCH /api/habits/{id} */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> update(
            @AuthenticationPrincipal User user,
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        String name      = (String) body.get("name");
        @SuppressWarnings("unchecked")
        List<String> days = (List<String>) body.get("targetDays");
        String lifeArea  = (String) body.get("lifeArea");
        Boolean active   = body.get("active") != null ? Boolean.parseBoolean(body.get("active").toString()) : null;
        return ResponseEntity.ok(ApiResponse.success("Habit updated",
                habitService.update(user.getId(), id, name, days, lifeArea, active)));
    }

    /** DELETE /api/habits/{id} (soft delete — sets active=false) */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> delete(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {
        habitService.delete(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Habit deleted", null));
    }

    /** POST /api/habits/{id}/log */
    @PostMapping("/{id}/log")
    public ResponseEntity<ApiResponse<Object>> logHabit(
            @AuthenticationPrincipal User user,
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        boolean completed = body.get("completed") != null
                && Boolean.parseBoolean(body.get("completed").toString());
        return ResponseEntity.ok(ApiResponse.success("Habit logged",
                habitService.log(user.getId(), id, completed)));
    }

    /** GET /api/habits/logs/today */
    @GetMapping("/logs/today")
    public ResponseEntity<ApiResponse<Object>> getTodayLogs(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(habitService.getTodayLogs(user.getId())));
    }

    /** GET /api/habits/logs/weekly */
    @GetMapping("/logs/weekly")
    public ResponseEntity<ApiResponse<Object>> getWeeklyLogs(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(habitService.getWeeklyLogs(user.getId())));
    }

    /** GET /api/habits/insights */
    @GetMapping("/insights")
    public ResponseEntity<ApiResponse<Object>> getInsights(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(habitService.getInsights(user.getId())));
    }
}
