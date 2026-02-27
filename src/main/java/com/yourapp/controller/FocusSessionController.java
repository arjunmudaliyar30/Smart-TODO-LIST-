package com.yourapp.controller;

import com.yourapp.dto.FocusSessionDTO;
import com.yourapp.model.FocusSession;
import com.yourapp.model.User;
import com.yourapp.dto.ApiResponse;
import com.yourapp.service.FocusSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Focus Session endpoints.
 *
 * POST /api/focus          — start a new session
 * GET  /api/focus          — list all sessions
 * POST /api/focus/{id}/complete — manually complete a session
 */
@RestController
@RequestMapping("/api/focus")
@RequiredArgsConstructor
public class FocusSessionController {

    private final FocusSessionService focusSessionService;

    /** Start a new focus session. Body: { durationMinutes: int, linkedTaskId?: string } */
    @PostMapping
    public ResponseEntity<ApiResponse<FocusSession>> startSession(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        int duration = Integer.parseInt(body.getOrDefault("durationMinutes", 25).toString());
        String linkedTaskId = body.containsKey("linkedTaskId") ? body.get("linkedTaskId").toString() : null;
        FocusSession session = focusSessionService.startSession(user.getId(), duration, linkedTaskId);
        return ResponseEntity.ok(ApiResponse.success("Focus session started", session));
    }

    /** List all focus sessions for the user. */
    @GetMapping
    public ResponseEntity<ApiResponse<List<FocusSessionDTO>>> getSessions(
            @AuthenticationPrincipal User user) {
        List<FocusSessionDTO> sessions = focusSessionService.getUserSessions(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Sessions retrieved", sessions));
    }

    /** Manually mark a session as complete. */
    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<FocusSession>> completeSession(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {
        FocusSession session = focusSessionService.completeSession(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Session completed", session));
    }
}
