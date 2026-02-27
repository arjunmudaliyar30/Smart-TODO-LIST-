package com.yourapp.controller;

import com.yourapp.dto.MoodAnalyticsDTO;
import com.yourapp.dto.StreakDTO;
import com.yourapp.dto.WeeklyReportDTO;
import com.yourapp.model.User;
import com.yourapp.dto.ApiResponse;
import com.yourapp.service.AchievementService;
import com.yourapp.service.MoodAnalyticsService;
import com.yourapp.service.StreakService;
import com.yourapp.service.WeeklyAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Analytics endpoints.
 *
 * GET /api/analytics/streaks
 * GET /api/analytics/weekly?weekDate=2026-02-24
 * GET /api/analytics/mood?from=2026-01-01&to=2026-01-31
 * GET /api/analytics/achievements
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final StreakService          streakService;
    private final WeeklyAnalyticsService weeklyAnalyticsService;
    private final MoodAnalyticsService   moodAnalyticsService;
    private final AchievementService     achievementService;

    /** Returns current streaks for the authenticated user. */
    @GetMapping("/streaks")
    public ResponseEntity<ApiResponse<StreakDTO>> getStreaks(
            @AuthenticationPrincipal User user) {
        StreakDTO dto = streakService.getStreakDTO(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Streaks retrieved", dto));
    }

    /**
     * Returns weekly analytics report.
     * @param weekDate any date in the desired week (defaults to today)
     */
    @GetMapping("/weekly")
    public ResponseEntity<ApiResponse<WeeklyReportDTO>> getWeeklyReport(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekDate) {
        WeeklyReportDTO report = weeklyAnalyticsService.generateWeeklyReport(user.getId(), weekDate);
        return ResponseEntity.ok(ApiResponse.success("Weekly report generated", report));
    }

    /**
     * Returns energy/mood analytics.
     * @param from start date (defaults to 30 days ago)
     * @param to   end date (defaults to today)
     */
    @GetMapping("/mood")
    public ResponseEntity<ApiResponse<MoodAnalyticsDTO>> getMoodAnalytics(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        MoodAnalyticsDTO dto = moodAnalyticsService.getAnalytics(user.getId(), from, to);
        return ResponseEntity.ok(ApiResponse.success("Mood analytics retrieved", dto));
    }

    /** Returns all achievements earned by the user. */
    @GetMapping("/achievements")
    public ResponseEntity<ApiResponse<?>> getAchievements(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Achievements retrieved",
                achievementService.getUserAchievements(user.getId())));
    }
}
