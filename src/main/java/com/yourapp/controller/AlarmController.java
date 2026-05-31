package com.yourapp.controller;

import com.yourapp.dto.ApiResponse;
import com.yourapp.model.Alarm;
import com.yourapp.model.User;
import com.yourapp.service.AlarmService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alarms")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmService alarmService;

    /** Create a standalone alarm. Body: { "title": "...", "scheduledAt": "2026-06-01T08:00:00" } */
    @PostMapping
    public ResponseEntity<ApiResponse<Alarm>> create(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {

        String title = body.getOrDefault("title", "Alarm");
        String scheduledAtStr = body.get("scheduledAt");
        if (scheduledAtStr == null || scheduledAtStr.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("scheduledAt is required"));
        }
        LocalDateTime scheduledAt = LocalDateTime.parse(scheduledAtStr);
        Alarm alarm = alarmService.create(user.getId(), title, scheduledAt);
        return ResponseEntity.ok(ApiResponse.success(alarm));
    }

    /** List all alarms for the current user. */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Alarm>>> list(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(alarmService.listForUser(user.getId())));
    }

    /**
     * Returns alarms that are NOW due for this user (fired between polls).
     * Marks them as fired so they won't be returned again.
     * Frontend should poll this every 30 seconds to trigger the overlay.
     */
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<Alarm>>> pending(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(alarmService.popPendingForUser(user.getId())));
    }

    /** Dismiss (stop) an alarm overlay — called when user presses STOP. */
    @PostMapping("/{id}/dismiss")
    public ResponseEntity<ApiResponse<Void>> dismiss(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {
        alarmService.dismiss(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Dismissed", null));
    }

    /** Delete an alarm. */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {
        alarmService.delete(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }
}
