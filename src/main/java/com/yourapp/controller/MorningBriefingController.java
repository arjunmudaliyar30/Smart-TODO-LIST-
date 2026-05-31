package com.yourapp.controller;

import com.yourapp.dto.ApiResponse;
import com.yourapp.model.User;
import com.yourapp.service.MorningBriefingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/briefing")
@RequiredArgsConstructor
public class MorningBriefingController {

    private final MorningBriefingService briefingService;

    /** GET /api/briefing/today — generate or return today's morning briefing */
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<Object>> getToday(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(briefingService.getTodayBriefing(user.getId())));
    }

    /** GET /api/briefing/history */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Object>> getHistory(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(briefingService.getHistory(user.getId())));
    }
}
