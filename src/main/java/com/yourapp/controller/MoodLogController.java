package com.yourapp.controller;

import com.yourapp.dto.ApiResponse;
import com.yourapp.model.User;
import com.yourapp.service.MoodLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mood")
@RequiredArgsConstructor
public class MoodLogController {

    private final MoodLogService moodLogService;

    /** POST /api/mood — log mood (energy/mood/focus 1-5 + optional voice note) */
    @PostMapping
    public ResponseEntity<ApiResponse<Object>> logMood(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        int energy    = toInt(body.get("energy"), 3);
        int mood      = toInt(body.get("mood"),   3);
        int focus     = toInt(body.get("focus"),  3);
        String note   = body.get("voiceNote") != null ? body.get("voiceNote").toString() : null;
        var saved = moodLogService.save(user.getId(), energy, mood, focus, note);
        return ResponseEntity.ok(ApiResponse.success("Mood logged", saved));
    }

    /** GET /api/mood/today */
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<Object>> getToday(@AuthenticationPrincipal User user) {
        return moodLogService.getToday(user.getId())
                .<ResponseEntity<ApiResponse<Object>>>map(ml -> ResponseEntity.ok(ApiResponse.success(ml)))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.success(null)));
    }

    /** GET /api/mood/weekly */
    @GetMapping("/weekly")
    public ResponseEntity<ApiResponse<Object>> getWeekly(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(moodLogService.getWeekly(user.getId())));
    }

    /** GET /api/mood/patterns */
    @GetMapping("/patterns")
    public ResponseEntity<ApiResponse<Object>> getPatterns(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(moodLogService.getPatterns(user.getId())));
    }

    private int toInt(Object val, int def) {
        if (val == null) return def;
        try { return Integer.parseInt(val.toString()); }
        catch (NumberFormatException e) { return def; }
    }
}
