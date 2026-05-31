package com.yourapp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Document(collection = "tasks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String title;

    private String description;

    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;

    private TaskPriority priority;

    @Indexed
    private LocalDateTime dueDate;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    private LocalDateTime completedAt;

    @Builder.Default
    private List<String> subTasks = new ArrayList<>();

    private String category;

    /** Freeform section/grouping label (e.g. "Morning Routine", "Sprint 1") */
    private String section;

    /** Link to a Goal document — optional */
    @Indexed
    private String goalId;

    /** Whether this task has been shared with collaborators */
    @Builder.Default
    private boolean shared = false;

    /** User IDs of collaborators who can view/complete this task */
    @Builder.Default
    private List<String> collaboratorIds = new ArrayList<>();

    @Builder.Default
    private boolean reminderSent = false;

    /** Optional scheduled date for calendar/planner view (Step 3) */
    @Indexed
    private LocalDate scheduledDate;

    /** FITNESS category sub-type — only relevant when category = "FITNESS" (Step 4) */
    private FitnessType fitnessType;

    /** Calories consumed on this task/activity (nullable, Step 5) */
    private Integer caloriesConsumed;

    /** Calories burned on this task/activity (nullable, Step 5) */
    private Integer caloriesBurned;

    // -----------------------------------------------------------------------
    // Fitness Category (Phase 12 — replaces plain FitnessType dropdown)
    // -----------------------------------------------------------------------

    /** ID reference to a FitnessCategory document (nullable). */
    private String fitnessCategoryId;

    // -----------------------------------------------------------------------
    // Nested task hierarchy
    // -----------------------------------------------------------------------

    /**
     * ID of a parent Task, allowing exercise nesting:
     * e.g. "Leg Workout" → Squats, Lunges, Deadlift.
     * Null means this is a top-level task.
     */
    @Indexed
    private String parentTaskId;

    // -----------------------------------------------------------------------
    // Auto-completion via duration timer
    // -----------------------------------------------------------------------

    /**
     * How long this task is expected to run, in minutes.
     * Required when autoComplete is true.
     */
    private Integer durationMinutes;

    /**
     * Set automatically to LocalDateTime.now() when task status moves
     * to IN_PROGRESS. Used by {@link com.yourapp.scheduler.AutoCompleteScheduler}.
     */
    private LocalDateTime startTime;

    /**
     * When true and durationMinutes is set, the AutoCompleteScheduler will
     * mark this task DONE once startTime + durationMinutes has elapsed.
     */
    private Boolean autoComplete;

    // -----------------------------------------------------------------------
    // Collaboration
    // -----------------------------------------------------------------------

    /**
     * When true, collaborators listed in collaboratorIds may update this task's
     * status and fields. When false, they can only view.
     */
    @Builder.Default
    private boolean collaborativeEditable = false;

    // -----------------------------------------------------------------------
    // Recurring (Daily) Task
    // -----------------------------------------------------------------------

    /** When true this task repeats daily and sends a notification at recurringTime. */
    @Builder.Default
    private boolean recurring = false;

    /** Daily reminder time in HH:mm format, e.g. "08:30". Only used when recurring=true. */
    private String recurringTime;

    /** Tracks the last date a daily notification was fired (prevents duplicate alerts). */
    private LocalDate recurringNotifiedDate;

    // -----------------------------------------------------------------------
    // Collaborator Progress Tracking
    // -----------------------------------------------------------------------

    /**
     * Maps collaboratorId (or ownerId) -> their personal completion progress for this task.
     * Each collaborator can independently report their % done and add a note.
     */
    @Builder.Default
    private Map<String, CollaboratorProgress> collaboratorProgress = new HashMap<>();

    // -----------------------------------------------------------------------
    // One-time Alarm
    // -----------------------------------------------------------------------

    /** Fire a single notification at this exact date-time. Null means no alarm. */
    private LocalDateTime alarmTime;

    /** Set to true once the alarm notification has been dispatched. */
    @Builder.Default
    private boolean alarmFired = false;

    /** Optional life area tag — references a LifeArea document id. */
    private String lifeAreaId;

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class CollaboratorProgress {
        /** 0–100 percent complete as reported by this collaborator. */
        private int completionPct;
        /** Optional note from the collaborator about what they did. */
        private String note;
        private LocalDateTime updatedAt;
    }

    public enum FitnessType {
        WORKOUT, DIET, CALORIES
    }

    public enum TaskStatus {
        PENDING, IN_PROGRESS, COMPLETED, CANCELLED,
        /** Alias for COMPLETED — use DONE in UI, maps to same logic */
        DONE
    }

    public enum TaskPriority {
        LOW, MEDIUM, HIGH, URGENT
    }
}
