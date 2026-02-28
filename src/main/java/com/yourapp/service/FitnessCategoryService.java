package com.yourapp.service;

import com.yourapp.model.FitnessCategory;
import com.yourapp.repository.FitnessCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class FitnessCategoryService {

    private final FitnessCategoryRepository fitnessCategoryRepository;

    /** System-level default categories seeded on startup — never editable by users. */
    private static final List<String> SYSTEM_DEFAULTS = List.of("CALORIES", "STRENGTH", "COMBAT");

    /**
     * Runs once after the application context is fully started.
     * Inserts missing system categories without touching user-created ones.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void seedSystemDefaults() {
        for (String name : SYSTEM_DEFAULTS) {
            if (!fitnessCategoryRepository.existsByNameAndUserIdIsNull(name)) {
                fitnessCategoryRepository.save(FitnessCategory.builder()
                        .name(name)
                        .userId(null)
                        .editable(false)
                        .build());
                log.info("Seeded system fitness category: {}", name);
            }
        }
    }

    /**
     * Returns all categories visible to the user:
     * system defaults (userId=null) + their own custom ones.
     */
    public List<FitnessCategory> getCategoriesForUser(String userId) {
        if (userId == null) return List.of();
        return fitnessCategoryRepository.findSystemAndUserCategories(userId);
    }

    /**
     * Creates a new user-owned custom fitness category.
     * Validates: non-blank name, no duplicate for this user.
     */
    public FitnessCategory createCustomCategory(String userId, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Category name must not be blank");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        String normalised = name.trim().toUpperCase();
        if (fitnessCategoryRepository.existsByNameAndUserIdIsNull(normalised)) {
            throw new IllegalArgumentException("'" + normalised + "' is a system category and cannot be duplicated");
        }
        if (fitnessCategoryRepository.existsByNameAndUserId(normalised, userId)) {
            throw new IllegalArgumentException("Category '" + normalised + "' already exists");
        }
        return fitnessCategoryRepository.save(FitnessCategory.builder()
                .name(normalised)
                .userId(userId)
                .editable(true)
                .build());
    }

    /**
     * Deletes a user-owned custom category.
     * System defaults (editable=false) cannot be deleted.
     */
    public void deleteCategory(String userId, String categoryId) {
        if (categoryId == null || userId == null) {
            throw new IllegalArgumentException("Category ID and user ID must not be null");
        }
        FitnessCategory cat = fitnessCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId));
        if (!cat.isEditable() || !userId.equals(cat.getUserId())) {
            throw new IllegalArgumentException("Cannot delete a system default category");
        }
        fitnessCategoryRepository.deleteById(categoryId);
    }
}
