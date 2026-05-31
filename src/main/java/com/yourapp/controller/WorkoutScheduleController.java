package com.yourapp.controller;

import com.yourapp.dto.ApiResponse;
import com.yourapp.dto.WorkoutScheduleRequest;
import com.yourapp.model.User;
import com.yourapp.model.WorkoutSchedule;
import com.yourapp.service.WorkoutScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workout-schedule")
@RequiredArgsConstructor
public class WorkoutScheduleController {

    private final WorkoutScheduleService scheduleService;

    @GetMapping
    public ResponseEntity<ApiResponse<WorkoutSchedule>> getSchedule(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {

        LocalDate monday = WorkoutScheduleService.toMonday(
                weekStart != null ? weekStart : LocalDate.now());
        WorkoutSchedule sched = scheduleService.getSchedule(user.getId(), monday);
        return ResponseEntity.ok(ApiResponse.success(sched));
    }

    /** GET all weeks (for monthly/history view). */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<WorkoutSchedule>>> getAllSchedules(
            @AuthenticationPrincipal User user) {
        List<WorkoutSchedule> all = scheduleService.getAllSchedules(user.getId());
        return ResponseEntity.ok(ApiResponse.success(all));
    }

    /** PUT — save (upsert) a full week plan. */
    @PutMapping
    public ResponseEntity<ApiResponse<WorkoutSchedule>> saveSchedule(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody WorkoutScheduleRequest req) {
        WorkoutSchedule saved = scheduleService.saveSchedule(user.getId(), req);
        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    @PostMapping("/push")
    public ResponseEntity<ApiResponse<Map<String, Object>>> pushToActive(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {

        LocalDate monday = WorkoutScheduleService.toMonday(
                weekStart != null ? weekStart : LocalDate.now());
        int count = scheduleService.pushToActiveWorkouts(user.getId(), monday);
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("message", "Pushed " + count + " workout(s) to active", "created", count)));
    }
}
