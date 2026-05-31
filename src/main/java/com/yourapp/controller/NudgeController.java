package com.yourapp.controller;

import com.yourapp.dto.ApiResponse;
import com.yourapp.model.User;
import com.yourapp.service.NudgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/nudges")
@RequiredArgsConstructor
public class NudgeController {

    private final NudgeService nudgeService;

    /** GET /api/nudges/log — get nudge history */
    @GetMapping("/log")
    public ResponseEntity<ApiResponse<Object>> getLog(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(nudgeService.getLogs(user.getId())));
    }
}
