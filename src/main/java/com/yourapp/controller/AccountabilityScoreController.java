package com.yourapp.controller;

import com.yourapp.dto.ApiResponse;
import com.yourapp.model.User;
import com.yourapp.service.AccountabilityScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/score")
@RequiredArgsConstructor
public class AccountabilityScoreController {

    private final AccountabilityScoreService scoreService;

    /** GET /api/score/today — returns (or generates) today's score */
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<Object>> getToday(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(scoreService.getToday(user.getId())));
    }

    /** POST /api/score/today/recalculate — recalculates score for today */
    @PostMapping("/today/recalculate")
    public ResponseEntity<ApiResponse<Object>> recalculate(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Score recalculated",
                scoreService.calculateAndSave(user.getId())));
    }

    /** GET /api/score/weekly */
    @GetMapping("/weekly")
    public ResponseEntity<ApiResponse<Object>> getWeekly(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(scoreService.getWeekly(user.getId())));
    }
}
