package com.yourapp.controller;

import com.yourapp.dto.ApiResponse;
import com.yourapp.dto.DailyCaloriesDTO;
import com.yourapp.dto.TaskRequest;
import com.yourapp.model.Task;
import com.yourapp.model.Task.TaskStatus;
import com.yourapp.model.User;
import com.yourapp.service.AiService;
import com.yourapp.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Slf4j
public class TaskController {

    private final TaskService taskService;
    private final AiService aiService;

    @PostMapping
    public ResponseEntity<ApiResponse<Task>> createTask(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody TaskRequest request) {
        Task task = taskService.createTask(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Task created", task));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Task>>> getAllTasks(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) TaskStatus status) {
        List<Task> tasks = status != null
                ? taskService.getTasksByStatus(user.getId(), status)
                : taskService.getUserTasks(user.getId());
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    /** Step 3: GET /api/tasks/by-date?date=YYYY-MM-DD */
    @GetMapping("/by-date")
    public ResponseEntity<ApiResponse<List<Task>>> getTasksByDate(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<Task> tasks = taskService.getTasksByDate(user.getId(), date);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    /** Step 5: GET /api/tasks/calories/net?date=YYYY-MM-DD */
    @GetMapping("/calories/net")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDailyNetCalories(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        int net = taskService.calculateDailyNetCalories(user.getId(), date);
        return ResponseEntity.ok(ApiResponse.success(Map.of("date", date.toString(), "netCalories", net)));
    }

    /** Phase 12: GET /api/tasks/calories/summary?date=YYYY-MM-DD — full DTO */
    @GetMapping("/calories/summary")
    public ResponseEntity<ApiResponse<DailyCaloriesDTO>> getDailyCaloriesSummary(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        DailyCaloriesDTO summary = taskService.calculateDailyCalories(user.getId(), date);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Task>> getTask(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(taskService.getTaskById(user.getId(), id)));
    }

    /** Phase 12: GET /api/tasks/{id}/subtasks — child tasks of a parent task */
    @GetMapping("/{id}/subtasks")
    public ResponseEntity<ApiResponse<List<Task>>> getSubTasks(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {
        List<Task> children = taskService.getSubTasksByParent(id);
        return ResponseEntity.ok(ApiResponse.success(children));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Task>> updateTask(
            @AuthenticationPrincipal User user,
            @PathVariable String id,
            @Valid @RequestBody TaskRequest request) {
        Task updated = taskService.updateTask(user.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Task updated", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {
        taskService.deleteTask(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Task deleted", null));
    }

    @PostMapping("/{id}/breakdown")
    public ResponseEntity<ApiResponse<Task>> generateBreakdown(
            @AuthenticationPrincipal User user,
            @PathVariable String id) throws IOException {
        Task task = taskService.getTaskById(user.getId(), id);
        List<String> subtasks = aiService.generateTaskBreakdown(task.getTitle(), task.getDescription());
        Task updated = taskService.addSubtasks(user.getId(), id, subtasks);
        return ResponseEntity.ok(ApiResponse.success("AI breakdown generated", updated));
    }

    /** PATCH /api/tasks/{id}/status -- cycle or set explicit status (supports collaborators) */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Task>> cycleStatus(
            @AuthenticationPrincipal User user,
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {

        TaskStatus explicitStatus = null;
        if (body != null && body.containsKey("status")) {
            try {
                explicitStatus = TaskStatus.valueOf(body.get("status"));
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid status value: " + body.get("status")));
            }
        }
        Task updated = taskService.cycleStatusForActor(user.getId(), id, explicitStatus);
        return ResponseEntity.ok(ApiResponse.success("Status updated", updated));
    }

    /** POST /api/tasks/{id}/collaborators — add a collaborator by email (resolves email→MongoDB ID) */
    @PostMapping("/{id}/collaborators")
    public ResponseEntity<ApiResponse<Task>> addCollaborator(
            @AuthenticationPrincipal User user,
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank())
            return ResponseEntity.badRequest().body(ApiResponse.error("Email is required"));
        Task updated = taskService.addCollaboratorByEmail(user.getId(), id, email.trim().toLowerCase());
        return ResponseEntity.ok(ApiResponse.success("Collaborator added", updated));
    }

    /** DELETE /api/tasks/{id}/leave — remove the current user from the task's collaborator list */
    @DeleteMapping("/{id}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveTask(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {
        taskService.leaveTask(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Left task successfully", null));
    }

    /** Step 2: PATCH /api/tasks/{id}/toggle-status -- no request body needed */
    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<ApiResponse<Task>> toggleStatus(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {
        Task updated = taskService.toggleStatus(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Status toggled", updated));
    }

    /**
     * PATCH /api/tasks/{id}/my-progress
     * Allows owner or any collaborator to record their personal completion % and note.
     */
    @PatchMapping("/{id}/my-progress")
    public ResponseEntity<ApiResponse<Task>> updateMyProgress(
            @AuthenticationPrincipal User user,
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        int pct = Integer.parseInt(body.getOrDefault("completionPct", "0").toString());
        String note = body.getOrDefault("note", "").toString();
        Task updated = taskService.updateCollaboratorProgress(user.getId(), id, pct, note);
        return ResponseEntity.ok(ApiResponse.success("Progress updated", updated));
    }

    /** GET /api/tasks/recurring — returns only the recurring (daily) tasks for the logged-in user. */
    @GetMapping("/recurring")
    public ResponseEntity<ApiResponse<List<Task>>> getRecurringTasks(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(taskService.getRecurringTasks(user.getId())));
    }
}
