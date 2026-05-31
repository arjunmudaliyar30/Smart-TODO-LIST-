package com.yourapp.controller;

import com.yourapp.dto.ApiResponse;
import com.yourapp.model.Session;
import com.yourapp.model.User;
import com.yourapp.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @PostMapping
    public ResponseEntity<ApiResponse<Session>> create(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {

        Session session = Session.builder()
                .userId(user.getId())
                .type(str(body, "type"))
                .durationMinutes(intVal(body, "durationMinutes"))
                .caloriesBurned(intVal(body, "caloriesBurned"))
                .notes(str(body, "notes"))
                .mood(str(body, "mood"))
                .sessionDate(body.containsKey("sessionDate")
                        ? LocalDate.parse(str(body, "sessionDate"))
                        : LocalDate.now())
                .build();

        return ResponseEntity.ok(ApiResponse.success(sessionService.create(session)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Session>>> list(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(sessionService.listForUser(user.getId())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {
        sessionService.delete(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }

    // ---- helpers ----
    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? null : v.toString();
    }
    private Integer intVal(Map<String, Object> m, String k) {
        Object v = m.get(k);
        if (v == null) return null;
        try { return Integer.parseInt(v.toString()); } catch (NumberFormatException e) { return null; }
    }
}
