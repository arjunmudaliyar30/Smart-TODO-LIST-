package com.yourapp.controller;

import com.yourapp.dto.ApiResponse;
import com.yourapp.dto.ExerciseRequest;
import com.yourapp.dto.WorkoutRequest;
import com.yourapp.model.User;
import com.yourapp.model.Workout;
import com.yourapp.model.Workout.ExerciseStatus;
import com.yourapp.model.Workout.WorkoutStatus;
import com.yourapp.model.Workout.WorkoutType;
import com.yourapp.service.WorkoutService;
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
@RequestMapping("/api/workouts")
@RequiredArgsConstructor
public class WorkoutController {

    private final WorkoutService workoutService;

    // ---- CRUD ----

    @PostMapping
    public ResponseEntity<ApiResponse<Workout>> createWorkout(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody WorkoutRequest request) {
        Workout workout = workoutService.createWorkout(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Workout created", workout));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Workout>>> getWorkouts(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) WorkoutType type,
            @RequestParam(required = false, defaultValue = "false") boolean archived,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        List<Workout> workouts;
        if (from != null && to != null) {
            workouts = workoutService.getWorkoutsInRange(user.getId(), from, to);
        } else if (type != null) {
            workouts = workoutService.getWorkoutsByType(user.getId(), type);
        } else if (archived) {
            workouts = workoutService.getArchivedWorkouts(user.getId());
        } else {
            workouts = workoutService.getActiveWorkouts(user.getId());
        }
        return ResponseEntity.ok(ApiResponse.success(workouts));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<Workout>>> getAllWorkouts(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(workoutService.getUserWorkouts(user.getId())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Workout>> getWorkout(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(workoutService.getWorkoutById(user.getId(), id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Workout>> updateWorkout(
            @AuthenticationPrincipal User user,
            @PathVariable String id,
            @Valid @RequestBody WorkoutRequest request) {
        Workout updated = workoutService.updateWorkout(user.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Workout updated", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteWorkout(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {
        workoutService.deleteWorkout(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Workout deleted", null));
    }

    // ---- STATUS ----

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Workout>> updateStatus(
            @AuthenticationPrincipal User user,
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        WorkoutStatus status = WorkoutStatus.valueOf(body.get("status").toUpperCase());
        Workout updated = workoutService.updateStatus(user.getId(), id, status);
        return ResponseEntity.ok(ApiResponse.success("Status updated", updated));
    }

    // ---- ARCHIVE ----

    @PatchMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<Workout>> toggleArchive(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {
        Workout updated = workoutService.toggleArchive(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(
                updated.isArchived() ? "Workout archived" : "Workout unarchived", updated));
    }

    // ---- COLLABORATORS ----

    @PostMapping("/{id}/collaborators")
    public ResponseEntity<ApiResponse<Workout>> addCollaborator(
            @AuthenticationPrincipal User user,
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Email required"));
        }
        Workout updated = workoutService.addCollaborator(user.getId(), id, email.trim());
        return ResponseEntity.ok(ApiResponse.success("Collaborator added", updated));
    }

    @DeleteMapping("/{id}/collaborators/{collaboratorId}")
    public ResponseEntity<ApiResponse<Workout>> removeCollaborator(
            @AuthenticationPrincipal User user,
            @PathVariable String id,
            @PathVariable String collaboratorId) {
        Workout updated = workoutService.removeCollaborator(user.getId(), id, collaboratorId);
        return ResponseEntity.ok(ApiResponse.success("Collaborator removed", updated));
    }

    // ---- EXERCISES ----

    @PostMapping("/{id}/exercises")
    public ResponseEntity<ApiResponse<Workout>> addExercise(
            @AuthenticationPrincipal User user,
            @PathVariable String id,
            @Valid @RequestBody ExerciseRequest request) {
        Workout updated = workoutService.addExercise(user.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Exercise added", updated));
    }

    @PutMapping("/{id}/exercises/{index}")
    public ResponseEntity<ApiResponse<Workout>> updateExercise(
            @AuthenticationPrincipal User user,
            @PathVariable String id,
            @PathVariable int index,
            @Valid @RequestBody ExerciseRequest request) {
        Workout updated = workoutService.updateExercise(user.getId(), id, index, request);
        return ResponseEntity.ok(ApiResponse.success("Exercise updated", updated));
    }

    @PatchMapping("/{id}/exercises/{index}/status")
    public ResponseEntity<ApiResponse<Workout>> updateExerciseStatus(
            @AuthenticationPrincipal User user,
            @PathVariable String id,
            @PathVariable int index,
            @RequestBody Map<String, String> body) {
        ExerciseStatus status = ExerciseStatus.valueOf(body.get("status").toUpperCase());
        Workout updated = workoutService.updateExerciseStatusByCollaborator(user.getId(), id, index, status);
        return ResponseEntity.ok(ApiResponse.success("Exercise status updated", updated));
    }

    @DeleteMapping("/{id}/exercises/{index}")
    public ResponseEntity<ApiResponse<Workout>> deleteExercise(
            @AuthenticationPrincipal User user,
            @PathVariable String id,
            @PathVariable int index) {
        Workout updated = workoutService.deleteExercise(user.getId(), id, index);
        return ResponseEntity.ok(ApiResponse.success("Exercise deleted", updated));
    }
}

