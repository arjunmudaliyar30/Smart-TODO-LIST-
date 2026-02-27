package com.yourapp.controller;

import com.yourapp.dto.ApiResponse;
import com.yourapp.dto.UserFitnessProfileRequest;
import com.yourapp.model.User;
import com.yourapp.model.UserFitnessProfile;
import com.yourapp.service.UserFitnessProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/fitness/profile")
@RequiredArgsConstructor
public class UserFitnessProfileController {

    private final UserFitnessProfileService service;

    @GetMapping
    public ResponseEntity<ApiResponse<UserFitnessProfile>> getProfile(
            @AuthenticationPrincipal User user) {
        UserFitnessProfile profile = service.getProfile(user.getId());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserFitnessProfile>> upsertProfile(
            @AuthenticationPrincipal User user,
            @RequestBody UserFitnessProfileRequest request) {
        UserFitnessProfile profile = service.upsertProfile(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile saved", profile));
    }

    /**
     * Compute calorie goal without saving — preview calculation.
     */
    @PostMapping("/compute")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> compute(
            @RequestBody UserFitnessProfileRequest request) {
        int goal = UserFitnessProfileService.computeCalorieGoal(
                request.getWeightKg(), request.getHeightCm(),
                request.getAge(), request.getGender(), request.getActivityLevel());
        return ResponseEntity.ok(ApiResponse.success(Map.of("dailyCalorieGoal", goal)));
    }
}
