package com.yourapp.scheduler;

import com.yourapp.repository.UserRepository;
import com.yourapp.service.WeeklyReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Generates AI weekly reviews every Sunday at 8 PM IST.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WeeklyReviewScheduler {

    private final UserRepository     userRepository;
    private final WeeklyReviewService weeklyReviewService;

    @Scheduled(cron = "0 0 20 * * SUN", zone = "Asia/Kolkata")
    public void runWeeklyReviews() {
        log.info("WeeklyReviewScheduler: generating weekly reviews for all users");
        try {
            userRepository.findAll().forEach(user -> {
                try {
                    weeklyReviewService.generateWeeklyReview(user.getId());
                } catch (Exception e) {
                    log.warn("WeeklyReviewScheduler error for userId={}: {}", user.getId(), e.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("WeeklyReviewScheduler top-level error: {}", e.getMessage());
        }
    }
}
