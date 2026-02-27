package com.yourapp.controller;

import com.yourapp.dto.FitnessProgressionDTO;
import com.yourapp.model.User;
import com.yourapp.dto.ApiResponse;
import com.yourapp.service.VolumeCalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Fitness Progression endpoints.
 *
 * GET /api/fitness/progression?exerciseName=Bench+Press
 */
@RestController
@RequestMapping("/api/fitness")
@RequiredArgsConstructor
public class FitnessProgressionController {

    private final VolumeCalculationService volumeCalculationService;

    /**
     * Returns weight progression and weekly volume for a given exercise.
     * @param exerciseName the exercise to query (case-insensitive partial match)
     */
    @GetMapping("/progression")
    public ResponseEntity<ApiResponse<FitnessProgressionDTO>> getProgression(
            @AuthenticationPrincipal User user,
            @RequestParam String exerciseName) {
        if (exerciseName == null || exerciseName.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("exerciseName is required"));
        }
        FitnessProgressionDTO dto = volumeCalculationService
                .getExerciseProgression(user.getId(), exerciseName);
        return ResponseEntity.ok(ApiResponse.success("Exercise progression retrieved", dto));
    }
}
