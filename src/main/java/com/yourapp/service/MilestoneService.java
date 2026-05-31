package com.yourapp.service;

import com.yourapp.model.Milestone;
import com.yourapp.repository.MilestoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Idempotent milestone awarding.
 * Use MilestoneType constants for the 'type' parameter.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class MilestoneService {

    // Predefined milestone types
    public static final String JOINED                = "JOINED";
    public static final String FIRST_TASK            = "FIRST_TASK";
    public static final String STREAK_7              = "STREAK_7";
    public static final String STREAK_30             = "STREAK_30";
    public static final String TASKS_100             = "TASKS_100";
    public static final String FIRST_WEEKLY_REVIEW   = "FIRST_WEEKLY_REVIEW";
    public static final String FIRST_REFLECTION      = "FIRST_REFLECTION";
    public static final String PHOTOS_30             = "PHOTOS_30";
    public static final String BRAIN_STREAK_7        = "BRAIN_STREAK_7";
    public static final String SCORE_90              = "SCORE_90";

    private final MilestoneRepository milestoneRepository;
    private final WebPushService      webPushService;

    /** Awards a milestone only if not already awarded. Fires a push notification. */
    public void checkAndAward(String userId, String type, String label) {
        if (milestoneRepository.existsByUserIdAndType(userId, type)) {
            return; // Already awarded — idempotent
        }
        Milestone milestone = Milestone.builder()
                .userId(userId)
                .type(type)
                .label(label)
                .date(LocalDate.now())
                .build();
        milestoneRepository.save(milestone); //NOSONAR

        try {
            webPushService.sendPush(userId, "🏆 Milestone Unlocked!", label);
        } catch (Exception e) {
            log.warn("MilestoneService push notification failed for userId={}: {}", userId, e.getMessage());
        }
    }

    public List<Milestone> getMilestones(String userId) {
        return milestoneRepository.findByUserIdOrderByDateAsc(userId);
    }
}
