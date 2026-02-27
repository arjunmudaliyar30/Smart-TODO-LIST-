package com.yourapp.dto;

import com.yourapp.model.Task.TaskPriority;
import com.yourapp.model.Task.TaskStatus;
import com.yourapp.model.Task.FitnessType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TaskRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private TaskStatus status;

    private TaskPriority priority;

    private LocalDateTime dueDate;

    private String category;

    /** Optional section/grouping label */
    private String section;

    /** Optional ID of the goal this task contributes towards */
    private String goalId;

    /** Whether to share with listed collaborators */
    private boolean shared;

    /** User IDs to share this task with */
    private List<String> collaboratorIds;

    /** Optional scheduled date for calendar planner (Step 3) */
    private LocalDate scheduledDate;

    /** FITNESS sub-type — only when category=FITNESS (Step 4, kept for backwards compat) */
    private FitnessType fitnessType;

    /** Calories consumed — must be >= 0 (Step 5) */
    @Min(value = 0, message = "caloriesConsumed must be >= 0")
    private Integer caloriesConsumed;

    /** Calories burned — must be >= 0 (Step 5) */
    @Min(value = 0, message = "caloriesBurned must be >= 0")
    private Integer caloriesBurned;

    // -----------------------------------------------------------------------
    // Phase 12 extensions
    // -----------------------------------------------------------------------

    /** ID of a FitnessCategory document (replaces plain FitnessType enum in UI). */
    private String fitnessCategoryId;

    /** ID of a parent Task for nested exercise hierarchy (e.g. Leg Workout → Squats). */
    private String parentTaskId;

    /**
     * Duration of the task in minutes. Must be >= 1 when provided.
     * Required when autoComplete is true.
     */
    @Min(value = 1, message = "durationMinutes must be >= 1")
    private Integer durationMinutes;

    /**
     * When true, the AutoCompleteScheduler will mark this task DONE once
     * startTime + durationMinutes has elapsed. Requires durationMinutes to be set.
     */
    private Boolean autoComplete;

    /**
     * Whether collaborators can edit this task (true) or only view it (false).
     */
    private boolean collaborativeEditable;

    /** When true this task repeats daily with a notification at recurringTime. */
    private boolean recurring;

    /** Daily reminder time in HH:mm (24h) format, e.g. "08:30". Required when recurring=true. */
    private String recurringTime;
}

