package com.yourapp.controller;

import com.yourapp.dto.ApiResponse;
import com.yourapp.dto.CaloriesLogRequest;
import com.yourapp.model.CaloriesLog;
import com.yourapp.model.User;
import com.yourapp.service.CaloriesLogService;
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
@RequestMapping("/api/calories")
@RequiredArgsConstructor
public class CaloriesLogController {

    private final CaloriesLogService service;

    @PostMapping
    public ResponseEntity<ApiResponse<CaloriesLog>> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CaloriesLogRequest request) {
        CaloriesLog log = service.create(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Entry logged", log));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CaloriesLog>>> getByDate(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date != null) {
            return ResponseEntity.ok(ApiResponse.success(service.getByDate(user.getId(), date)));
        }
        return ResponseEntity.ok(ApiResponse.success(service.getAll(user.getId())));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> summary(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Map<String, Integer> summary = service.getDailySummary(user.getId(), date);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CaloriesLog>> update(
            @AuthenticationPrincipal User user,
            @PathVariable String id,
            @Valid @RequestBody CaloriesLogRequest request) {
        CaloriesLog log = service.update(user.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Entry updated", log));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {
        service.delete(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Entry deleted", null));
    }
}
