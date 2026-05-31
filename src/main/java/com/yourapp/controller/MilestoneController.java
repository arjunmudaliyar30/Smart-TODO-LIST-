package com.yourapp.controller;

import com.yourapp.dto.ApiResponse;
import com.yourapp.model.User;
import com.yourapp.service.MilestoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/milestones")
@RequiredArgsConstructor
public class MilestoneController {

    private final MilestoneService milestoneService;

    /** GET /api/milestones */
    @GetMapping
    public ResponseEntity<ApiResponse<Object>> getMilestones(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(milestoneService.getMilestones(user.getId())));
    }
}
