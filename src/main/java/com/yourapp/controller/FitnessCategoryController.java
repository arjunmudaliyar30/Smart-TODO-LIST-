package com.yourapp.controller;

import com.yourapp.dto.ApiResponse;
import com.yourapp.model.FitnessCategory;
import com.yourapp.model.User;
import com.yourapp.service.FitnessCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Manages fitness categories (system defaults + user custom ones).
 * Base path: /api/fitness/categories
 */
@RestController
@RequestMapping("/api/fitness/categories")
@RequiredArgsConstructor
public class FitnessCategoryController {

    private final FitnessCategoryService fitnessCategoryService;

    /** GET /api/fitness/categories — returns system + user categories */
    @GetMapping
    public ResponseEntity<ApiResponse<List<FitnessCategory>>> getCategories(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(
                fitnessCategoryService.getCategoriesForUser(user.getId())));
    }

    /**
     * POST /api/fitness/categories
     * Body: { "name": "MY_CATEGORY" }
     */
    @PostMapping
    public ResponseEntity<ApiResponse<FitnessCategory>> createCategory(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {
        String name = body == null ? null : body.get("name");
        FitnessCategory cat = fitnessCategoryService.createCustomCategory(user.getId(), name);
        return ResponseEntity.ok(ApiResponse.success("Category created", cat));
    }

    /** DELETE /api/fitness/categories/{id} — only user-owned, editable categories */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {
        fitnessCategoryService.deleteCategory(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Category deleted", null));
    }
}
