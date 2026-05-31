package com.yourapp.scheduler;

import com.yourapp.repository.UserRepository;
import com.yourapp.service.NudgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs hourly to check nudge conditions for all users.
 * Max 2 nudges/day/user enforced inside NudgeService.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NudgeScheduler {

    private final UserRepository userRepository;
    private final NudgeService   nudgeService;

    @Scheduled(fixedRate = 3_600_000) // every hour
    public void runNudges() {
        log.info("NudgeScheduler: checking nudge conditions for all users");
        try {
            userRepository.findAll().forEach(user -> {
                try {
                    nudgeService.checkAndNudge(user.getId());
                } catch (Exception e) {
                    log.warn("NudgeScheduler error for userId={}: {}", user.getId(), e.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("NudgeScheduler top-level error: {}", e.getMessage());
        }
    }
}
